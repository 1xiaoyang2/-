package com.heima.utils.thread;


import com.heima.model.common.user.pojos.ApUser;
import com.heima.model.wemedia.pojos.WmUser;

public class AppThreadLocalUtils {
    private final static ThreadLocal<ApUser> App_USER_THREAD_LOCAL = new ThreadLocal<>();

    public final static void setUser(ApUser apUser){
        App_USER_THREAD_LOCAL.set(apUser);
    }

    public final static ApUser getUser(){
        return App_USER_THREAD_LOCAL.get();
    }

    public final static void clear(){
        App_USER_THREAD_LOCAL.remove();
    }
}
