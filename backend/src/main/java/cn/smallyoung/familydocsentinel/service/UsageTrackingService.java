package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.config.FactCheckProperties;
import cn.smallyoung.familydocsentinel.dto.UserUsageDto;
import cn.smallyoung.familydocsentinel.entity.UsageLogEntity;
import cn.smallyoung.familydocsentinel.entity.UserUsageStatEntity;
import cn.smallyoung.familydocsentinel.exception.BizException;
import cn.smallyoung.familydocsentinel.repository.UsageLogRepository;
import cn.smallyoung.familydocsentinel.repository.UserUsageStatRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 资源用量（大模型 Token 与元宝搜索次数）双链额度管控、统计与审计服务
 * <p>
 * 支持按每日、每周、每月三个时间维度对 Token 消耗与元宝搜索次数进行双链统计与熔断拦截，
 * 任一链路在任一周期达到额度上限即停止服务。
 * </p>
 */
@Slf4j
@Service
public class UsageTrackingService {

    @Resource
    private UserUsageStatRepository userUsageStatRepository;

    @Resource
    private UsageLogRepository usageLogRepository;

    @Resource
    private FactCheckProperties factCheckProperties;

    /**
     * 校验用户双链额度（Token & 元宝搜索 · 日/周/月）是否充足；任一额度耗尽即抛出业务异常停止服务
     *
     * @param userId 当前登录用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkUserQuotaAvailable(Long userId) {
        if (userId == null) {
            return;
        }
        UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                .orElseGet(() -> buildInitialStat(userId));

        boolean changed = alignPeriodCounters(stat);
        if (changed && stat.getId() != null) {
            userUsageStatRepository.save(stat);
        }

        String exhaustedReason = evaluateQuotaExhaustionReason(stat);
        if (exhaustedReason != null) {
            log.warn("[UserQuotaAlert] 用户 [{}] 触发额度熔断停止服务: {}", userId, exhaustedReason);
            throw BizException.of(exhaustedReason);
        }
    }

    /**
     * 记录大模型 Token 消耗并累加日/周/月及总用量
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordModelUsage(Long userId, String taskId, String action,
                                 int promptTokens, int completionTokens, long costTimeMs) {
        int totalTokens = promptTokens + completionTokens;
        try {
            // 1. 记流水日志
            UsageLogEntity logEntity = UsageLogEntity.builder()
                    .userId(userId)
                    .taskId(taskId)
                    .serviceType("DEEPSEEK_AI")
                    .callAction(action)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .searchCount(0)
                    .costTimeMs(costTimeMs)
                    .build();
            usageLogRepository.save(logEntity);

            // 2. 如果已登录，累加用户总计与日/周/月统计
            if (userId != null && totalTokens > 0) {
                UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                        .orElseGet(() -> buildInitialStat(userId));

                alignPeriodCounters(stat);

                stat.setTotalPromptTokens(safeLong(stat.getTotalPromptTokens()) + promptTokens);
                stat.setTotalCompletionTokens(safeLong(stat.getTotalCompletionTokens()) + completionTokens);
                stat.setTotalTokens(safeLong(stat.getTotalTokens()) + totalTokens);

                stat.setDailyTokens(safeLong(stat.getDailyTokens()) + totalTokens);
                stat.setWeeklyTokens(safeLong(stat.getWeeklyTokens()) + totalTokens);
                stat.setMonthlyTokens(safeLong(stat.getMonthlyTokens()) + totalTokens);

                userUsageStatRepository.save(stat);
            }
        } catch (Exception e) {
            log.error("记录大模型 Token 用量异常: {}", e.getMessage());
        }
    }

    /**
     * 记录腾讯元宝搜索调用次数并累加日/周/月及总用量
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordSearchUsage(Long userId, String taskId, int searchCount, long costTimeMs) {
        try {
            // 1. 记流水日志
            UsageLogEntity logEntity = UsageLogEntity.builder()
                    .userId(userId)
                    .taskId(taskId)
                    .serviceType("TENCENT_WSA_SEARCH")
                    .callAction("WSA_WEB_SEARCH")
                    .promptTokens(0)
                    .completionTokens(0)
                    .totalTokens(0)
                    .searchCount(searchCount)
                    .costTimeMs(costTimeMs)
                    .build();
            usageLogRepository.save(logEntity);

            // 2. 如果已登录，累加用户总计与日/周/月统计
            if (userId != null && searchCount > 0) {
                UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                        .orElseGet(() -> buildInitialStat(userId));

                alignPeriodCounters(stat);

                stat.setTotalSearchCount(safeLong(stat.getTotalSearchCount()) + searchCount);
                stat.setDailySearchCount(safeLong(stat.getDailySearchCount()) + searchCount);
                stat.setWeeklySearchCount(safeLong(stat.getWeeklySearchCount()) + searchCount);
                stat.setMonthlySearchCount(safeLong(stat.getMonthlySearchCount()) + searchCount);

                userUsageStatRepository.save(stat);
            }
        } catch (Exception e) {
            log.error("记录元宝搜索用量异常: {}", e.getMessage());
        }
    }

    /**
     * 累加用户发起的核验任务数
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementTaskCount(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                    .orElseGet(() -> buildInitialStat(userId));
            alignPeriodCounters(stat);
            stat.setTaskCount((stat.getTaskCount() != null ? stat.getTaskCount() : 0) + 1);
            userUsageStatRepository.save(stat);
        } catch (Exception e) {
            log.error("累加用户核验任务数异常: {}", e.getMessage());
        }
    }

    /**
     * 查询用户用量与日/周/月双链额度概览
     */
    @Transactional(rollbackFor = Exception.class)
    public UserUsageDto getUserUsage(Long userId) {
        FactCheckProperties.UserQuota defaultQuota = getDefaultQuota();
        if (userId == null) {
            return UserUsageDto.builder()
                    .totalTokens(0L)
                    .totalPromptTokens(0L)
                    .totalCompletionTokens(0L)
                    .totalSearchCount(0L)
                    .taskCount(0)
                    .dailyTokensUsed(0L)
                    .dailyTokenLimit(defaultQuota.getDailyTokenLimit())
                    .dailySearchUsed(0L)
                    .dailySearchLimit(defaultQuota.getDailySearchLimit())
                    .weeklyTokensUsed(0L)
                    .weeklyTokenLimit(defaultQuota.getWeeklyTokenLimit())
                    .weeklySearchUsed(0L)
                    .weeklySearchLimit(defaultQuota.getWeeklySearchLimit())
                    .monthlyTokensUsed(0L)
                    .monthlyTokenLimit(defaultQuota.getMonthlyTokenLimit())
                    .monthlySearchUsed(0L)
                    .monthlySearchLimit(defaultQuota.getMonthlySearchLimit())
                    .quotaExceeded(false)
                    .build();
        }

        UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                .orElseGet(() -> buildInitialStat(userId));

        boolean changed = alignPeriodCounters(stat);
        if (changed && stat.getId() != null) {
            userUsageStatRepository.save(stat);
        }

        long dailyTokenLimit = resolveLimit(stat.getDailyTokenLimit(), defaultQuota.getDailyTokenLimit());
        long weeklyTokenLimit = resolveLimit(stat.getWeeklyTokenLimit(), defaultQuota.getWeeklyTokenLimit());
        long monthlyTokenLimit = resolveLimit(stat.getMonthlyTokenLimit(), defaultQuota.getMonthlyTokenLimit());

        long dailySearchLimit = resolveLimit(stat.getDailySearchLimit(), defaultQuota.getDailySearchLimit());
        long weeklySearchLimit = resolveLimit(stat.getWeeklySearchLimit(), defaultQuota.getWeeklySearchLimit());
        long monthlySearchLimit = resolveLimit(stat.getMonthlySearchLimit(), defaultQuota.getMonthlySearchLimit());

        String exhaustedReason = evaluateQuotaExhaustionReason(stat);

        return UserUsageDto.builder()
                .userId(userId)
                .totalTokens(safeLong(stat.getTotalTokens()))
                .totalPromptTokens(safeLong(stat.getTotalPromptTokens()))
                .totalCompletionTokens(safeLong(stat.getTotalCompletionTokens()))
                .totalSearchCount(safeLong(stat.getTotalSearchCount()))
                .taskCount(stat.getTaskCount() != null ? stat.getTaskCount() : 0)
                .dailyTokensUsed(safeLong(stat.getDailyTokens()))
                .dailyTokenLimit(dailyTokenLimit)
                .dailySearchUsed(safeLong(stat.getDailySearchCount()))
                .dailySearchLimit(dailySearchLimit)
                .weeklyTokensUsed(safeLong(stat.getWeeklyTokens()))
                .weeklyTokenLimit(weeklyTokenLimit)
                .weeklySearchUsed(safeLong(stat.getWeeklySearchCount()))
                .weeklySearchLimit(weeklySearchLimit)
                .monthlyTokensUsed(safeLong(stat.getMonthlyTokens()))
                .monthlyTokenLimit(monthlyTokenLimit)
                .monthlySearchUsed(safeLong(stat.getMonthlySearchCount()))
                .monthlySearchLimit(monthlySearchLimit)
                .quotaExceeded(exhaustedReason != null)
                .quotaExceededReason(exhaustedReason)
                .updatedAt(stat.getUpdatedAt() != null ? stat.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    /**
     * 对齐日/周/月周期游标：跨周期自动归零，存量记录首次访问时从审计日志回填本周期真实消耗
     */
    private boolean alignPeriodCounters(UserUsageStatEntity stat) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        boolean modified = false;

        // 1. 每日周期对齐
        if (stat.getDailyPeriodDate() == null) {
            if (stat.getId() != null && usageLogRepository != null) {
                LocalDateTime dayStartTime = today.atStartOfDay();
                stat.setDailyTokens(usageLogRepository.sumTotalTokensByUserIdAndCreatedAtAfter(stat.getUserId(), dayStartTime));
                stat.setDailySearchCount(usageLogRepository.sumSearchCountByUserIdAndCreatedAtAfter(stat.getUserId(), dayStartTime));
            } else {
                stat.setDailyTokens(safeLong(stat.getDailyTokens()));
                stat.setDailySearchCount(safeLong(stat.getDailySearchCount()));
            }
            stat.setDailyPeriodDate(today);
            modified = true;
        } else if (!today.equals(stat.getDailyPeriodDate())) {
            stat.setDailyTokens(0L);
            stat.setDailySearchCount(0L);
            stat.setDailyPeriodDate(today);
            modified = true;
        }

        // 2. 每周周期对齐（自然周周一 00:00 起）
        if (stat.getWeeklyPeriodDate() == null) {
            if (stat.getId() != null && usageLogRepository != null) {
                LocalDateTime weekStartTime = weekStart.atStartOfDay();
                stat.setWeeklyTokens(usageLogRepository.sumTotalTokensByUserIdAndCreatedAtAfter(stat.getUserId(), weekStartTime));
                stat.setWeeklySearchCount(usageLogRepository.sumSearchCountByUserIdAndCreatedAtAfter(stat.getUserId(), weekStartTime));
            } else {
                stat.setWeeklyTokens(safeLong(stat.getWeeklyTokens()));
                stat.setWeeklySearchCount(safeLong(stat.getWeeklySearchCount()));
            }
            stat.setWeeklyPeriodDate(weekStart);
            modified = true;
        } else if (!weekStart.equals(stat.getWeeklyPeriodDate())) {
            stat.setWeeklyTokens(0L);
            stat.setWeeklySearchCount(0L);
            stat.setWeeklyPeriodDate(weekStart);
            modified = true;
        }

        // 3. 每月周期对齐（自然月 1 日 00:00 起）
        if (stat.getMonthlyPeriodDate() == null) {
            if (stat.getId() != null && usageLogRepository != null) {
                LocalDateTime monthStartTime = monthStart.atStartOfDay();
                stat.setMonthlyTokens(usageLogRepository.sumTotalTokensByUserIdAndCreatedAtAfter(stat.getUserId(), monthStartTime));
                stat.setMonthlySearchCount(usageLogRepository.sumSearchCountByUserIdAndCreatedAtAfter(stat.getUserId(), monthStartTime));
            } else {
                stat.setMonthlyTokens(safeLong(stat.getMonthlyTokens()));
                stat.setMonthlySearchCount(safeLong(stat.getMonthlySearchCount()));
            }
            stat.setMonthlyPeriodDate(monthStart);
            modified = true;
        } else if (!monthStart.equals(stat.getMonthlyPeriodDate())) {
            stat.setMonthlyTokens(0L);
            stat.setMonthlySearchCount(0L);
            stat.setMonthlyPeriodDate(monthStart);
            modified = true;
        }

        return modified;
    }

    /**
     * 评估双链三周期额度是否已耗尽：按 每日 -> 每周 -> 每月 顺序检查 Token 链与元宝搜索链，哪个够了即返回停止服务提示
     */
    private String evaluateQuotaExhaustionReason(UserUsageStatEntity stat) {
        FactCheckProperties.UserQuota defaultQuota = getDefaultQuota();

        long dailyTokens = safeLong(stat.getDailyTokens());
        long dailyTokenLimit = resolveLimit(stat.getDailyTokenLimit(), defaultQuota.getDailyTokenLimit());
        if (dailyTokens >= dailyTokenLimit) {
            return String.format("今日 Token 额度已用完（%d / %d），已停止服务，请明日再试", dailyTokens, dailyTokenLimit);
        }

        long dailySearch = safeLong(stat.getDailySearchCount());
        long dailySearchLimit = resolveLimit(stat.getDailySearchLimit(), defaultQuota.getDailySearchLimit());
        if (dailySearch >= dailySearchLimit) {
            return String.format("今日元宝搜索次数额度已用完（%d / %d 次），已停止服务，请明日再试", dailySearch, dailySearchLimit);
        }

        long weeklyTokens = safeLong(stat.getWeeklyTokens());
        long weeklyTokenLimit = resolveLimit(stat.getWeeklyTokenLimit(), defaultQuota.getWeeklyTokenLimit());
        if (weeklyTokens >= weeklyTokenLimit) {
            return String.format("本周 Token 额度已用完（%d / %d），已停止服务，请下周再试", weeklyTokens, weeklyTokenLimit);
        }

        long weeklySearch = safeLong(stat.getWeeklySearchCount());
        long weeklySearchLimit = resolveLimit(stat.getWeeklySearchLimit(), defaultQuota.getWeeklySearchLimit());
        if (weeklySearch >= weeklySearchLimit) {
            return String.format("本周元宝搜索次数额度已用完（%d / %d 次），已停止服务，请下周再试", weeklySearch, weeklySearchLimit);
        }

        long monthlyTokens = safeLong(stat.getMonthlyTokens());
        long monthlyTokenLimit = resolveLimit(stat.getMonthlyTokenLimit(), defaultQuota.getMonthlyTokenLimit());
        if (monthlyTokens >= monthlyTokenLimit) {
            return String.format("本月 Token 额度已用完（%d / %d），已停止服务，请下月再试", monthlyTokens, monthlyTokenLimit);
        }

        long monthlySearch = safeLong(stat.getMonthlySearchCount());
        long monthlySearchLimit = resolveLimit(stat.getMonthlySearchLimit(), defaultQuota.getMonthlySearchLimit());
        if (monthlySearch >= monthlySearchLimit) {
            return String.format("本月元宝搜索次数额度已用完（%d / %d 次），已停止服务，请下月再试", monthlySearch, monthlySearchLimit);
        }

        return null;
    }

    private UserUsageStatEntity buildInitialStat(Long userId) {
        LocalDate today = LocalDate.now();
        return UserUsageStatEntity.builder()
                .userId(userId)
                .totalPromptTokens(0L)
                .totalCompletionTokens(0L)
                .totalTokens(0L)
                .totalSearchCount(0L)
                .taskCount(0)
                .dailyTokens(0L)
                .weeklyTokens(0L)
                .monthlyTokens(0L)
                .dailySearchCount(0L)
                .weeklySearchCount(0L)
                .monthlySearchCount(0L)
                .dailyPeriodDate(today)
                .weeklyPeriodDate(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                .monthlyPeriodDate(today.with(TemporalAdjusters.firstDayOfMonth()))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private FactCheckProperties.UserQuota getDefaultQuota() {
        if (factCheckProperties != null && factCheckProperties.getUserQuota() != null) {
            return factCheckProperties.getUserQuota();
        }
        return new FactCheckProperties.UserQuota();
    }

    private static long resolveLimit(Long customLimit, long defaultLimit) {
        return (customLimit != null && customLimit > 0) ? customLimit : defaultLimit;
    }

    private static long safeLong(Long val) {
        return val != null ? val : 0L;
    }
}

