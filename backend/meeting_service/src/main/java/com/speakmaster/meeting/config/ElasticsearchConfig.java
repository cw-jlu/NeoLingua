package com.speakmaster.meeting.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch配置
 * 
 * @author SpeakMaster
 */
@Slf4j
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:localhost}")
    private String esHost;

    @Value("${elasticsearch.port:9200}")
    private int esPort;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            // 创建低级客户端
            RestClient restClient = RestClient.builder(
                new HttpHost(esHost, esPort, "http")
            ).build();

            // 创建传输层
            ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
            );

            // 创建API客户端
            ElasticsearchClient client = new ElasticsearchClient(transport);
            
            log.info("Elasticsearch客户端初始化成功: {}:{}", esHost, esPort);
            return client;
        } catch (Exception e) {
            log.error("Elasticsearch客户端初始化失败", e);
            throw new RuntimeException("无法连接到Elasticsearch", e);
        }
    }
}
