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
    public void searchData() throws IOException {
        // 1.创建检索要求
        SearchRequest searchRequest = new SearchRequest();
        // 指定检索
        searchRequest.indices("bank");
        // 指定DSL,检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));

        // 年龄聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        sourceBuilder.aggregation(ageAgg);

        // 计算平均薪资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        sourceBuilder.aggregation(balanceAvg);

        System.out.println("检索条件 = " + sourceBuilder.toString());

        searchRequest.source(sourceBuilder);

        // 2.执行检索
        SearchResponse searchResponse = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        // 3.分析结果，searchResponse
        System.out.println("searchResponse = " + searchResponse);

        // 3.1获取所有的数据
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            String string = hit.getSourceAsString();
            Account account = JSON.parseObject(string, Account.class);
            System.out.println("account = " + account);
        }

        // 3.2 获取这次检索到的分析信息
        Aggregations aggregations = searchResponse.getAggregations();

        Terms ageAggRep = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAggRep.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄 = " + keyAsString + "==>" + bucket.getDocCount());
        }

        // 薪资
        Avg balanceAvgRe = aggregations.get("balanceAvg");
        System.out.println("平均薪资 = " + balanceAvgRe.getValue());

    }

    /**
     * 测试保存数据
     */
    @Test
    public void testIndexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User user = new User();
        user.setUserName("肖一");
        user.setAge(18);
        user.setGender("男");
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
