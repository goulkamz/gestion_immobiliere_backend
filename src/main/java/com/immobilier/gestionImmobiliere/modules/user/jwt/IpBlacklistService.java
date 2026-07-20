package com.immobilier.gestionImmobiliere.modules.user.jwt;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IpBlacklistService {

    private static final Duration DUREE_BLACKLIST = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    public IpBlacklistService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void blacklister(String ip, String raison) {
        String cle = "blacklist:ip:" + ip;
        stringRedisTemplate.opsForValue().set(cle, raison, DUREE_BLACKLIST);
    }

    public boolean estBlackliste(String ip) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey("blacklist:ip:" + ip));
    }
}