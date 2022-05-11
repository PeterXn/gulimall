package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1.查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 2.组装成父子的树形结构

        // 2.1 找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());


        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单，是否被别的地方引用

        // 逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[parentPath.size()]);
    }


    /**
     * 级联更新所有的数据
     *
     * @param category
     * @CacheEvict: 失效模式
     * @CachePut: 双写模式
     * 1、同时实现多种缓存操作@Caching
     * 2、指定删除某个分区的所有数据，@CacheEvict(value = "category",allEntries = true)
     * 3、存储同一类型的数据，都可以指定成同一个分区。
     *
     */

    //@Caching(evict = {
    //        @CacheEvict(value = "category", key = "'getLevel1Categorys'"),
    //        @CacheEvict(value = "category", key = "'getCatalogJson'")
    //})

    @CacheEvict(value = "category",allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        // 更新级联表
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 1）、代表当前方法需要缓存，如果缓存中有则方法不调用。
     * 2）、@Cacheable("category")，指定缓存的分区
     * 3)、默认行为：
     * （1）、如果缓存中有，则方法不调用
     * （2）、key默认自动生成，缓存的名字：category::SimpleKey []
     * （3）、缓存的value值，默认使用jdk序列化机制，钭序列化后的数据保存到redis.
     * <p>
     * 4）、自定义：
     * （1）、指定生成的缓存使用的key; 使用Key属性，接受一个SpEL
     * (2)、指定缓存的数据的存活时间；配置文件修改ttl
     * (3)、将数据保存为json格式; CacheAutoConfiguration->RedisCacheConfiguration
     *
     * 4、spring-Cache的不足
     *   1）、读模式：
     *      缓存穿透：查询一个不存在的数据。-> 缓存空数据 spring.cache.redis.cache-null-values=true
     *      缓存击穿：大量并发同时查询一个正好过期的数据。 -> ?，解决方法：@Cacheable(sync = true)
     *      缓存雪崩：大量的key同时过期。 -> 加上过期时间。spring.cache.redis.time-to-live=3600000
     *   2）、写模式：(缓存与数据库一致)
     *      1. 读写加锁
     *      2. 引入Canal,感知到mysql的更新去更新缓存
     *      3. 读多写多，直接去查询数据库
     *
     *   总结：常规数据（读多写少、即时性、一致性要求不高的数据）；完成使用spring-cache
     *        特殊数据，特殊设计。
     *
     * @return
     */
    @Cacheable(value = "category", key = "#root.method.name",sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("getLevel1Categorys......");
        final Long LEVEL1 = 0L;
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", LEVEL1));

        return categoryEntities;
    }

    /**
     * 使用注解@Cacheable实现缓存
     */
    @Cacheable(value = "category", key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        System.out.println("注解实现->查询了数据库......");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        // 1.查出所有1级分类,parentCid = 0L
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        // 2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 1.每一个的一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            // 封装成指定的格式
                            Catelog2Vo.Catelog3Vo category3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }

                    return catelog2Vo;

                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        return parent_cid;
    }


    /**
     * TODO: io.netty.util.internal.OutOfDirectMemoryError
     * <p>
     * 1. springboot2.0以后默认使用lettuce作为操作redis的客户端；它使用netty进行网络通信；
     * 2. lettuce的bug导致netty堆外内存溢出；-Xmx100m：netty没有指定堆外内存，默认使用-Xmx100m。
     * 解决方案：
     * 1. 升级lettuce客户端。 2.切换使用jedis
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonBk() {

        /**
         *  1. 高并发下缓存失效问题：缓存穿透 -> 空结果缓存
         *  2. 缓存同时失效：缓存雪崩 -> 随机过期时间
         *  3. 缓存击穿 （热点key可能会在某些时间点上超高并发的询问）-> 加锁
         */

        // 1、加入缓存逻辑，缓存中的数据都是json字符串；好处：跨平台、跨语言
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            // 2、缓存中没有则查询数据库
            System.out.println("缓存未命中....查询数据库....");
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();

            return catalogJsonFromDb;
        }

        // 缓存中存在，则转为特定的对象
        System.out.println("缓存命中....直接返回....");
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,
                new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });
        return result;
    }

    /**
     * 使用redisson分布式锁
     * 缓存数据一致性问题：
     * 1）、双写模式，
     * 2）、失效模式；
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        // 1. 锁的名字，越细越好，
        RLock lock = redisson.getLock("CatalogJson-lock");
        lock.lock();

        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }

        return dataFromDb;
    }

    /**
     * 使用redis分布式锁
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        // 1. 抢占分布式锁;去redis占坑
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功......");
            // 加锁成功: 2. 设置过期时间与加锁必须是同步的，原子操作
            //redisTemplate.expire("key", 30, TimeUnit.SECONDS);

            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                // 用lua脚本实现解锁; KEYS[1] -> lock; ARGV[1] -> uuid
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Long delResult = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"), uuid);
            }

            // 获取值对比与删除必须是同步操作，原子的。此方法不可行
            //String lockValue = redisTemplate.opsForValue().get("lock");
            //if (uuid.equals(lockValue)) {
            //    // 删除自己的锁
            //    redisTemplate.delete("key");
            //}


            return dataFromDb;
        } else {
            // 回销失败，重试；自旋
            System.out.println("获取分布式锁失败...等待重试");
            try {
                Thread.sleep(200);
            } catch (Exception e) {
            }
            return getCatalogJsonFromDbWithRedisLock();
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        // 得到锁以后，我们应该再去缓存中确定一次，如果没有才去查询数据库
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            // 缓存不为空，直接返回
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,
                    new TypeReference<Map<String, List<Catelog2Vo>>>() {
                    });
            return result;
        }

        System.out.println("查询了数据库......");

        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 1.查出所有1级分类,parentCid = 0L
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        // 2.封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 1.每一个的一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            // 封装成指定的格式
                            Catelog2Vo.Catelog3Vo category3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return category3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }

                    return catelog2Vo;

                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        // 3、将查询到的数据再放入缓存，将对象转为json放入缓存中
        String s = JSON.toJSONString(parent_cid);
        // 缓存过期时间为12小时
        redisTemplate.opsForValue().set("catalogJSON", s, 12, TimeUnit.HOURS);

        return parent_cid;
    }

    /**
     * 从数据库查询并封装分类数据
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {

        // 只要是同一把锁，就能锁住需要这个锁的所有进程；synchronized (this)，springboot所有的组件在容器中都是单例的。
        synchronized (this) {
            // 得到锁以后，我们应该再去缓存中确定一次，如果没有才去查询数据库
            return getDataFromDb();
        }

    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parentCid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> {
            return item.getParentCid().equals(parentCid);
        }).collect(Collectors.toList());

        return collect;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        // 收集当前节点
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    /**
     * 递归查找所有菜单的子菜单
     *
     * @return
     */
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            //return categoryEntity.getParentCid() == root.getCatId();
            return categoryEntity.getParentCid().equals(root.getCatId());
        }).map(categoryEntity -> {
            // 1.找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            // 2.菜单排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }

}