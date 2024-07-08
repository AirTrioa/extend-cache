package org.example.extend.cache.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * @author liuzw
 * @date 2024/7/5
 */
@EnableCaching
@SpringBootApplication
public class ExtendCacheDemo {
  public static void main(String[] args) {
    SpringApplication.run(ExtendCacheDemo.class, args);
  }
}
