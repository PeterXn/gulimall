package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.SkuSaleAttrValueDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemAttrGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Thread;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    SkuSaleAttrValueDao skuSaleAttrValueDao;

    @Test
    public void testSkuSaleAttrValueDao() {
        List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(11L);
        System.out.println("saleAttrsBySpuId = " + saleAttrsBySpuId);
    }

    @Test
    public void testAttrGroup() {
        List<SpuItemAttrGroupVo> group = attrGroupDao.getAttrGroupWithAttrsBySpuId(111L, 225L);
        System.out.println("group = " + group);

    }

    @Test
    public void testRedisson() {
        System.out.println("redissonClient = " + redissonClient);
    }

    @Test
    public void testRedis() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello", "world_" + UUID.randomUUID().toString());

        String hello = ops.get("hello");
        System.out.println(hello);

    }

    @Test
    public void testFindPath() {
        Long[] catelogPath = categoryService.findCatelogPath(225L);
        log.info("???????????????{}", Arrays.asList(catelogPath));
    }

    @Test
    public void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();

        //brandEntity.setName("??????");
        //brandService.save(brandEntity);
        //System.out.println("???????????? ...");

        brandEntity.setBrandId(1L);
        brandEntity.setDescript("????????????");
        brandService.updateById(brandEntity);
    }

    @Test
    public void test01() {
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("name", "??????"));
        list.forEach((item) ->
                System.out.println("item = " + item)
        );
    }

}
