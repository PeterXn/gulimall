package com.atguigu.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {

    @Autowired
    RestHighLevelClient client;

    @Data
    class User {
        private String userName;
        private String gender;
        private Integer age;
    }

    @Data
    @ToString
    public static class Account {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }


    @Test
    public void testStr() {
        String str = "keyword=%E5%8D%8E%E4%B8%BA&attrs=5_A2639;A2638;A2637";
        String attr = "&attrs=5_A2639;A2638;A2637";
        String replace = str.replace(attr, "");
        System.out.println("replace = " + replace);
    }

    @Test
    public void searchData() throws IOException {
        // 1.??????????????????
        SearchRequest searchRequest = new SearchRequest();
        // ????????????
        searchRequest.indices("bank");
        // ??????DSL,????????????
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));

        // ????????????
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);

        // ??????????????????
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(balanceAvg);

        System.out.println("???????????? = " + sourceBuilder.toString());

        searchRequest.source(sourceBuilder);

        // 2.????????????
        SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        // 3.???????????????searchResponse
        System.out.println("searchResponse = " + searchResponse);

        // 3.1?????????????????????
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            String string = hit.getSourceAsString();
            Account account = JSON.parseObject(string, Account.class);
            System.out.println("account = " + account);
        }

        // 3.2 ????????????????????????????????????
        Aggregations aggregations = searchResponse.getAggregations();

        Terms ageAggRep = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAggRep.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("?????? = " + keyAsString + "==>" + bucket.getDocCount());
        }

        // ??????
        Avg balanceAvgRe = aggregations.get("balanceAvg");
        System.out.println("???????????? = " + balanceAvgRe.getValue());

    }

    /**
     * ??????????????????
     */
    @Test
    public void testIndexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User user = new User();
        user.setUserName("??????");
        user.setAge(18);
        user.setGender("???");
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON);

        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        System.out.println("index = " + index);
    }


    @Test
    public void contextLoads() {
        System.out.println("restHighLevelClient = " + client);
    }

}
