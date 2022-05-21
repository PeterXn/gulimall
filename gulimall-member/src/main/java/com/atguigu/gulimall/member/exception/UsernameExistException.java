package com.atguigu.gulimall.member.exception;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/19 1:34
 * @description Usage
 */
public class UsernameExistException extends RuntimeException{

    public UsernameExistException() {
        super("用户名已存在");
    }
}
