package com.speakmaster.common.context;

/**
 * 用户上下文持有者
 * 使用 ThreadLocal 存储当前请求的用户信息
 * 
 * @author SpeakMaster
 */
public class UserContextHolder {
    
    private static final ThreadLocal<UserContext> CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置用户上下文
     * 
     * @param userContext 用户上下文
     */
    public static void setContext(UserContext userContext) {
        CONTEXT_HOLDER.set(userContext);
    }
    
    /**
     * 获取用户上下文
     * 
     * @return 用户上下文
     */
    public static UserContext getContext() {
        return CONTEXT_HOLDER.get();
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }
    
    /**
     * 获取当前用户名
     * 
     * @return 用户名
     */
    public static String getCurrentUsername() {
        UserContext context = getContext();
        return context != null ? context.getUsername() : null;
    }
    
    /**
     * 清除用户上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
