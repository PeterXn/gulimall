package com.atguigu.gulimall.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallAuthServerApplicationTests {

    @Test
    public void contextLoads() {
        String s = String.valueOf(System.currentTimeMillis());

        System.out.println("s = " + s.substring(7, 13));
        System.out.println("s = " + s);
    }

}
