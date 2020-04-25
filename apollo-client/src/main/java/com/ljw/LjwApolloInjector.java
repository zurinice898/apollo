package com.ljw;

import com.ctrip.framework.apollo.internals.Injector;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;

/**
 * @Author lujiawei
 * @Description
 * @Date 2020/4/19
 **/
public class LjwApolloInjector {
    private static volatile Injector s_injector;
    private static final Object lock = new Object();

    private static Injector getInjector(){
        if(s_injector == null){
            synchronized (lock){
                if(s_injector == null){
                    s_injector = ServiceBootstrap.loadFirst(Injector.class);
                }
            }
        }
        return s_injector;
    }
}
