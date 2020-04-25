package com.ljw;

/**
 * @Author lujiawei
 * @Description
 * @Date 2020/4/19
 **/
public class LjwApolloConfigException  extends RuntimeException{
    public LjwApolloConfigException(String message) {
        super(message);
    }

    public LjwApolloConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
