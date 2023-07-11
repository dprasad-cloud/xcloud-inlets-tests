package com.extremecloudiq.inlets.tests.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;

@Slf4j
@Configuration
public class RedisConfig {
    @Value("${device.connect.redis.host:default-redis-cluster:6379,default-redis-cluster:6379}")
    private String redisHost;

    @Bean
    RedisConnectionFactory redisConnectionFactory() {

        if (!redisHost.contains(",")) {
            String host = redisHost.split(":")[0].trim();
            int port = Integer.parseInt(redisHost.split(":")[1].trim());
            return new LettuceConnectionFactory(host, port);
        }

        Collection<String> redisNodes = Arrays.asList(redisHost.split(","));
        log.info("Cluster nodes: " + redisNodes);

        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(redisNodes);
        return new LettuceConnectionFactory(clusterConfig, buildLettuceClusterClientConfiguration());

    }

    private LettuceClientConfiguration buildLettuceClusterClientConfiguration() {
        return LettucePoolingClientConfiguration.builder()
            .build();
    }

    @Bean
    RedisTemplate<String, String> redisTemplate() {
        final RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericToStringSerializer<>(Long.class));
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        return template;
    }

    @PostConstruct
    public void init() {

    }


}
