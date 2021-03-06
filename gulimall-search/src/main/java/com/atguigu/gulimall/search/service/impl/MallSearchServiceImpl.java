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
     * ???es??????
     *
     * @param param ?????????????????????
     * @return
     */
    @Override
    public SearchResult search(SearchParam param) {
        // ??????????????????????????????dsl??????
        SearchResult result = null;

        //1.??????????????????
        SearchRequest searchRequest = buildSearchRequest(param);

        try {
            //2.??????????????????
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            //3.?????????????????????????????????????????????
            result = buildSearchResult(response, param);

        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }

    /**
     * ??????????????????
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????,??????????????????
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        //??????dsl??????
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /*
          ??????????????????????????????????????????????????????????????????????????????????????????
         */
        //1??? ??????bool query
        BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
        //1.1 ???must-????????????
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // 1.2???bool-filter - ????????????????????????
        if (param.getCatalog3Id() != null) {
            boolBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.2???bool-filter - ????????????id??????
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.2???bool-filter - ??????????????????
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                //attrs=1_5???:8???&2_16G:8G
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //????????????????????????Nested??????
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolBuilder.filter(nestedQuery);
            }

        }

        // 1.2???bool-filter - ???????????????????????????
        if (param.getHasStock() != null) {
            boolBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        // 1.2???bool-filter - ????????????????????????  skuPrice=1_500/_500/500_
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            //skuPrice=1_500/_500/500_
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                // 1_500??????; if s[0] > s[1] ?
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

        // ???????????????????????????
        sourceBuilder.query(boolBuilder);


        /*
          ??????????????????????????????
         */
        // 2.1?????????
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            // sort=saleCount_asc/desc
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
            //sourceBuilder.sort(s[0], SortOrder.fromString(s[1]));
        }

        // 2.2?????????
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3?????????
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }

        /*
          ??????????????????
         */
        // 3.1???????????????
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);

        // 3.1.1???????????????????????????
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));

        sourceBuilder.aggregation(brand_agg);

        ////////////////////////////////////////////////////////

        // 3.2??????????????? catalog_agg
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));

        sourceBuilder.aggregation(catalog_agg);

        ////////////////////////////////////////////////////////

        // 3.3??????????????? attr_agg
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        //???????????????????????? attrId
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        //????????????????????? attr_id ???????????????
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //????????????????????? attr_id ????????????????????????????????? attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));

        attr_agg.subAggregation(attr_id_agg);

        sourceBuilder.aggregation(attr_agg);

        ////////////////////////////////////////////////////////

        String s = sourceBuilder.toString();
        System.out.println("?????????DSL?????????" + s);


        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    /**
     * ??????????????????
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result = new SearchResult();

        //1????????????????????????????????????
        SearchHits hits = response.getHits();

        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                // ????????????
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(string);
                }

                esModels.add(esModel);
            }
        }

        result.setProducts(esModels);

        //2???????????????????????????????????????????????????
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");

        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // ??????id
            long attrId = bucket.getKeyAsNumber().longValue();
            // ??????????????? attr_name_agg
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // ???????????? attr_value_agg
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

        //3???????????????????????????????????????????????????
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // ??????ID
            long brandId = bucket.getKeyAsNumber().longValue();
            // ???????????? brand_name_agg
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            // ???????????? brand_img_agg
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }

        result.setBrands(brandVos);

        //4???????????????????????????????????????????????????
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");

        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        List<? extends Terms.Bucket> buckets = catalog_agg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //????????????id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            // catalog_agg??????????????????
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);

            catalogVos.add(catalogVo);

        }

        result.setCatalogs(catalogVos);

        //=========??????????????????????????????===============================================

        //5???????????????????????????????????????
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


        //6?????????????????? ????????????
        if (!CollectionUtils.isEmpty(param.getAttrs())) {
            // 1. ????????????attr??????????????????????????????
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                // attr???15_??????
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));

                // ??????????????????????????????????????????????????????????????????
                result.getAttrIds().add(Long.parseLong(s[0]));

                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(data.getAttrName());
                } else {
                    // ???id???????????????
                    navVo.setNavName(s[0]);
                }

                // 2. ?????????????????????????????????????????????????????????????????????url??????????????????
                //??????????????????????????????????????????
                String replace = replaceQueryString(param, attr,"attrs");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);

                return navVo;

            }).collect(Collectors.toList());

            result.setNavs(navVos);
        }


        // ????????????????????????
        if (!CollectionUtils.isEmpty(param.getBrandId())) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("??????");
            // TODO ????????????????????????
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
                // ??????link
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
            }

            navs.add(navVo);

            result.setNavs(navs);
        }

        // TODO ????????????????????????????????????????????????

        return result;
    }

    /**
     * ???????????????????????????: &attrs=5_A2639;A2638;A2637
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
            //??????????????????????????????Java???????????????????????????
            if (value.contains(" ")) {
                temp = encode.replace("+", "%20");
                encode = temp;
            }
            //??????????????????
            if (value.contains(";")) {
                temp = encode.replace("%3B", ";");
                encode = temp;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // "?attrs=" or "&attrs="?????????????????????
        if (param.get_queryString().contains("&")) {
            replace = param.get_queryString().replace("&" + key + "=" + encode, "");
        } else if (param.get_queryString().contains("?")) {
            replace = param.get_queryString().replace("?" + key + "=" + encode, "");
        }

        return replace;
    }


}
