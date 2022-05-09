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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

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
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        // 更新级联表
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }


    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        final Long LEVEL1 = 0L;
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", LEVEL1));

        return categoryEntities;
    }

    /**
     * TODO: io.netty.util.internal.OutOfDirectMemoryError
     * <p>
     * 1. springboot2.0以后默认使用lettuce作为操作redis的客户端；它使用netty进行网络通信；
     * 2. lettuce的bug导致netty堆外内存溢出；-Xmx100m：netty没有指定堆外内存，默认使用-Xmx100m。
     * 解决方案：
     * 1. 升级lettuce客户端。 2.切换使用jedis
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {

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
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();

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
     * 从数据库查询并封装分类数据
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {

        // 只要是同一把锁，就能锁住需要这个锁的所有进程；synchronized (this)，springboot所有的组件在容器中都是单例的。
        synchronized (this) {
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
            /*
            1. 将数据库的多次查询变为一次
             */
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
            redisTemplate.opsForValue().set("catalogJSON", s,6, TimeUnit.HOURS);

            return parent_cid;
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