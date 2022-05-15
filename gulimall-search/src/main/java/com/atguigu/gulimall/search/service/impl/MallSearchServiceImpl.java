package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.feign.ProductFeignService;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.AttrResponseVo;
import com.atguigu.gulimall.search.vo.BrandVo;
import com.atguigu.gulimall.search.vo.SearchParam;
import com.atguigu.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * To change it use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @date 2022/5/12 16:45
 * @description Usage
 */
@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;


    @Autowired
    ProductFeignService productFeignService;


    /**
     * 去es检索
     *
     * @param param 检索的所有参数
     * @return
     */
    @Override
    public SearchResult search(SearchParam param) {
        // 动态构建出查询需要的dsl语句
        SearchResult result = null;

        //1.准备检索请求
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            //2.执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            //3.分析响应数据封装我们需要的格式
            result = buildSearchResult(response, param);

        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }

    /**
     * 准备检索请求
     * 模糊匹配，过滤（按照属性、分类、品牌，价格区间，库存），完成排序、分页、高亮,聚合分析功能
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        //构造dsl语句
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /*
          查询：模糊匹配，过滤（按照属性、分类、品牌，价格区间，库存）
         */
        //1、 构建bool query
        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
        //1.1 、must-模糊匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // 1.2、bool-filter - 按照三级分类查询
        if (param.getCatalog3Id() != null) {
            boolBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.2、bool-filter - 按照品牌id查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.2、bool-filter - 按照属性查询
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                //attrs=1_5寸:8寸&2_16G:8G
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //每一个必须都生成Nested查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolBuilder.filter(nestedQuery);
            }

        }

        // 1.2、bool-filter - 按照是否有库存查询
        if (param.getHasStock() != null) {
            boolBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        // 1.2、bool-filter - 按照价格区别检索  skuPrice=1_500/_500/500_
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            //skuPrice=1_500/_500/500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                // 1_500区间; if s[0] > s[1] ?
                rangeQuery.gte(s[0]).lte(s[1]);
            } else if (s.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQuery.lte(s[0]);
                }

                if (param.getSkuPrice().endsWith("_")) {
                    rangeQuery.gte(s[0]);
                }
            }

            boolBuilder.filter(rangeQuery);
        }

        // 查询条件想构建完成
        sourceBuilder.query(boolBuilder);


        /*
          完成排序、分页、高亮
         */
        // 2.1、排序
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            // sort=saleCount_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
            //sourceBuilder.sort(s[0], SortOrder.fromString(s[1]));
        }

        // 2.2、分页
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3、高亮
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }

        /*
          聚合分析功能
         */
        // 3.1、品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);

        // 3.1.1、品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));

        sourceBuilder.aggregation(brand_agg);

        ////////////////////////////////////////////////////////

        // 3.2、分类聚合 catalog_agg
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));

        sourceBuilder.aggregation(catalog_agg);

        ////////////////////////////////////////////////////////

        // 3.3、属性聚合 attr_agg
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //聚合出当前所有的 attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //聚合分析出当前 attr_id 对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //聚合分析出当前 attr_id 对应的所有可能的属性值 attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));

        attr_agg.subAggregation(attr_id_agg);

        sourceBuilder.aggregation(attr_agg);

        ////////////////////////////////////////////////////////

        String s = sourceBuilder.toString();
        System.out.println("构造的DSL语句：" + s);


        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    /**
     * 构造结果数据
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result = new SearchResult();

        //1、返回的所有查询到的商品
        SearchHits hits = response.getHits();

        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                // 设置高亮
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }

                esModels.add(esModel);
            }
        }

        result.setProducts(esModels);

        //2、返回的查询到的商品的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");

        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // 属性id
            long attrId = bucket.getKeyAsNumber().longValue();
            // 属性的名字 attr_name_agg
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // 属性的值 attr_value_agg
            List<String> attrValues = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());

            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);

        //3、返回的查询到的商品的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // 品牌ID
            long brandId = bucket.getKeyAsNumber().longValue();
            // 品牌名字 brand_name_agg
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            // 品牌图片 brand_img_agg
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);

        //4、返回的查询到的商品的所有分类信息
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");

        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            // catalog_agg聚合的子聚合
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);

            catalogVos.add(catalogVo);
        }

        result.setCatalogs(catalogVos);

        //=========以上从聚合信息中获取===============================================

        //5、返回分页、总记录数、页码
        result.setPageNum(param.getPageNum());
        Long total = hits.getTotalHits().value;
        result.setTotal(total);
        int totalPages = total % EsConstant.PRODUCT_PAGESIZE == 0 ? (int) (total / EsConstant.PRODUCT_PAGESIZE) : ((int) (total / EsConstant.PRODUCT_PAGESIZE + 1));
        result.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);


        //6、构建面包屑 属性导航
        if (!CollectionUtils.isEmpty(param.getAttrs())) {
            // 1. 分析每个attr传递过来的查询参数值
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                // attr：15_海思
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));

                // 面包屑导航显示的属性，在下面的属性中则不显示
                result.getAttrIds().add(Long.parseLong(s[0]));

                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                } else {
                    // 以id设置属性名
                    navVo.setNavName(s[0]);
                }

                // 2. 取消面包屑导航后，页面跳转到哪？将请求地址中的url当前条件置空
                //拿到所有的查询条件，去掉当前
                String replace = replaceQueryString(param, attr,"attrs");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);

                return navVo;

            }).collect(Collectors.toList());

            result.setNavs(navVos);
        }


        // 品牌的面包屑导航
        if (!CollectionUtils.isEmpty(param.getBrandId())) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            // TODO 远程查询所有品牌
            R r = productFeignService.brandsInfo(param.getBrandId());
            if (r.getCode() == 0) {
                List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>() {
                });
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandVo brandVo : brand) {
                    buffer.append(brandVo.getName() + ";");
                    replace = replaceQueryString(param, brandVo.getBrandId() + "", "brandId");
                }
                navVo.setNavValue(buffer.toString());
                // 设置link
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
            }

            navs.add(navVo);

            result.setNavs(navs);
        }

        // TODO 分类的面包屑导航；不需要导航取消


        return result;
    }

    /**
     * 带分号的会替换失败: &attrs=5_A2639;A2638;A2637
     *
     * @param param
     * @param value
     * @param key
     * @return
     */
    private String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null;
        String replace = null;
        try {
            String temp = null;
            encode = URLEncoder.encode(value, "UTF-8");
            //浏览器对空格的编码和Java不一样。空格的处理
            if (value.contains(" ")) {
                temp = encode.replace("+", "%20");
                encode = temp;
            }
            //分号的的处理
            if (value.contains(";")) {
                temp = encode.replace("%3B", ";");
                encode = temp;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // "?attrs=" or "&attrs="；提前过滤分号
        if (param.get_queryString().contains("&")) {
            replace = param.get_queryString().replace("&" + key + "=" + encode, "");
        } else if (param.get_queryString().contains("?")) {
            replace = param.get_queryString().replace("?" + key + "=" + encode, "");
        }

        return replace;
    }


}
