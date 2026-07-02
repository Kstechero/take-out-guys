package test;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@SpringBootTest
public class SpringDataRedisTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testRedisTemplate() {
        redisTemplate.opsForValue().set("test:name", "sky");
        Object value = redisTemplate.opsForValue().get("test:name");

        System.out.println("Redis test:name = " + value);
        assertEquals("sky", value);
    }
}
