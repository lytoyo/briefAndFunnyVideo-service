package com.lytoyo.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

/**
 * Package:com.lytoyo.framework.config
 *  elasticsearch配置类
 * @ClassName:ElasticsearchConfig
 * @Create:2026/1/4 15:36
 **/
@Configuration
@Slf4j
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    @Value(value = "${spring.elasticsearch.rest.uris}")
    private String uris;

    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(uris)
                //如果需要SSL配置
                //.usingSsl()
                //如果需要认证
                //.withBasicAuth()
                .build();
        return RestClients.create(clientConfiguration).rest();
    }

    @Bean
    public ElasticsearchRestTemplate elasticsearchRestTemplate(){
        ElasticsearchRestTemplate elasticsearchRestTemplate = null;
        try {
            elasticsearchRestTemplate = new ElasticsearchRestTemplate(elasticsearchClient());
            log.info("Elasticsearch初始化成功");
        }catch (Exception e){
            log.error("Elasticsearch初始化失败:",e);
        }finally {
            return elasticsearchRestTemplate;
        }
    }
}
