package com.immobilier.gestionImmobiliere.modules.user.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /**
     * Template générique — pour tout usage futur nécessitant du polymorphisme
     * (plusieurs types d'objets possibles sous la même clé/pattern).
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Template typé pour RefreshTokenData — sérialiseur fixé sur ce type précis,
     * donc aucun champ "@class" écrit dans le JSON stocké en Redis.
     */
    @Bean
    public RedisTemplate<String, RefreshTokenRedisService.RefreshTokenData> refreshTokenRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, RefreshTokenRedisService.RefreshTokenData> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<RefreshTokenRedisService.RefreshTokenData> typedSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, RefreshTokenRedisService.RefreshTokenData.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(typedSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Template String pur — pour l'index user -> tokenIds (rt:user:xxx),
     * qui ne stocke que des chaînes simples, jamais d'objets sérialisés.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}