package cn.smallyoung.familydocsentinel.security;

/**
 * 异步任务执行上下文持有者（传递当前任务关联的用户 ID 与任务 ID）
 */
public final class TaskExecutionHolder {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TASK_ID = new ThreadLocal<>();

    private TaskExecutionHolder() {
    }

    public static void set(Long userId, String taskId) {
        if (userId != null) {
            CURRENT_USER_ID.set(userId);
        } else {
            CURRENT_USER_ID.remove();
        }
        if (taskId != null) {
            CURRENT_TASK_ID.set(taskId);
        } else {
            CURRENT_TASK_ID.remove();
        }
    }

    public static Long getUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId != null) {
            return userId;
        }
        return UserContext.getUserId();
    }

    public static String getTaskId() {
        return CURRENT_TASK_ID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        CURRENT_TASK_ID.remove();
    }
}

