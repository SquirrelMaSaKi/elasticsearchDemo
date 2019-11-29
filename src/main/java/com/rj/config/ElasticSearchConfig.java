package com.rj.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {
    @Value("${elasticsearch.host}")
    private String hostname;

    @Value("${elasticsearch.port}")
    private int port;

    /**
     * 创建一个RestHighLevelClient,后面的所有增删改操作都是要通过这个类
     * 为其制定地址和端口号
     */
    @Bean
    public RestHighLevelClient client() {
        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port));
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
}
