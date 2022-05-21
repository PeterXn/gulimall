package com.atguigu.common.to;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/19 21:17
 * @description 社交登录To类
 */
@Data
public class SocialUserTo {

    private String accessToken;
    private String remindIn;
    private long expiresIn;
    private String uid;
    private String isRealName;
}
