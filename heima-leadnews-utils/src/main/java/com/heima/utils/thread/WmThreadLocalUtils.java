package com.heima.utils.thread;


import com.heima.model.wemedia.pojos.WmUser;

public class WmThreadLocalUtils {
    private final static ThreadLocal<WmUser> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    public final static void setUser(WmUser wmUser){
        WM_USER_THREAD_LOCAL.set(wmUser);
    }

    public final static WmUser getUser(){
        return WM_USER_THREAD_LOCAL.get();
    }

    public final static void clear(){
        WM_USER_THREAD_LOCAL.remove();
    }
}
