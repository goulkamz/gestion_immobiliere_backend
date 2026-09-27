package com.immobilier.gestionImmobiliere.modules.user.jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RedisConfig {

    // Dates en ISO-8601 (et non en timestamps) : format deja utilise pour les tokens stockes
    private static JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    /**
     * Template générique — pour tout usage futur nécessitant du polymorphisme
     * (plusieurs types d'objets possibles sous la même clé/pattern).
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper());

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

        JacksonJsonRedisSerializer<RefreshTokenRedisService.RefreshTokenData> typedSerializer =
                new JacksonJsonRedisSerializer<>(jsonMapper(), RefreshTokenRedisService.RefreshTokenData.class);

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
