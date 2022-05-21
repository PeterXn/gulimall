package com.atguigu.gulimall.member.exception;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/19 1:34
 * @description Usage
 */
public class PhoneExistException extends RuntimeException{

    public PhoneExistException() {
        super("手机号码已存在");
    }
}
