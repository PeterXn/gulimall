package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import com.atguigu.common.utils.HttpUtils;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallThirdPartyApplicationTests {

    @Resource
    OSSClient ossClient;

    @Autowired
    SmsComponent smsComponent;

    @Test
    public void contextLoads() {
        smsComponent.sendSmsCode("12314725812", "123456");
    }

    @Test
    public void sendSms() {
        String host = "https://fesms.market.alicloudapi.com";
        String path = "/sms/";
        String method = "GET";
        String appcode = "b0563c23aff8428c97a54d4f7f9e28e5";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("code", "12345678");
        querys.put("phone", "15580220501");
        querys.put("skin", "1");
        querys.put("sign", "175622");
        //JDK 1.8示例代码请在这里下载：  http://code.fegine.com/Tools.zip
        try {
            /**
             * 重要提示如下:
             * HttpUtils请从
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
             * 或者直接下载：
             * http://code.fegine.com/HttpUtils.zip
             * 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             * 相关jar包（非pom）直接下载：
             * http://code.fegine.com/aliyun-jar.zip
             */
            HttpResponse response = HttpUtils.doGet(host, path, method, headers, querys);
            //System.out.println(response.toString());如不输出json, 请打开这行代码，打印调试头部状态码。
            //状态码: 200 正常；400 URL无效；401 appCode错误； 403 次数用完； 500 API网管错误
            //获取response的body
            System.out.println("response = " + response);
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpload() throws FileNotFoundException {
        //        // Endpoint以杭州为例，其它Region请按实际情况填写。
        //        String endpoint = "oss-cn-beijing.aliyuncs.com";
        //// 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建RAM账号。
        //        String accessKeyId = "LTAI4G6sWhsMNcDrwdR7mKck";
        //        String accessKeySecret = "ODyHrcm0AuMLobxHWl4IWM2vsqnh65";
        //
        //// 创建OSSClient实例。
        //        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 创建PutObjectRequest对象。
        //        PutObjectRequest putObjectRequest = new PutObjectRequest("gulimall-hello-3", "b.jpg", new File("/Users/mxg/Downloads/b.jpg"));

        // 如果需要上传时设置存储类型与访问权限，请参考以下示例代码。
        // ObjectMetadata metadata = new ObjectMetadata();
        // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
        // metadata.setObjectAcl(CannedAccessControlList.Private);
        // putObjectRequest.setMetadata(metadata);

        // 上传文件。
        ossClient.putObject("gulimall-hello-3", "a.jpg", new FileInputStream("D:\\Users\\Peter\\Pictures\\111.png"));


        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传完成....");
    }

}
