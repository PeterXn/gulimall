package com.atguigu.gulimall.auth.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/18 17:42
 * @description Usage
 */
@Data
public class UserRegistVo {

    @NotEmpty(message = "用户名不能为空")
    @Length(min=6,max = 10,message = "用户名必须是6-10位字符")
    private String userName;

    @NotEmpty(message = "密码不能为空")
    @Length(min=6,max = 10,message = "密码必须在是6-10位之间")
    private String password;

    @NotEmpty(message = "手机号码不能为空")
    @Pattern(regexp = "^[1]([3-9])[0-9]{9}$",message = "手机号码格式不正确")
    private String phone;

    @NotEmpty(message = "验证码不能为空")
    private String code;
}
