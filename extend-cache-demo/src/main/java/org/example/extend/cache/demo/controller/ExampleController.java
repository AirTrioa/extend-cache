package org.example.extend.cache.demo.controller;

import com.example.definition.Example;
import com.example.definition.RestResult;
import org.example.extend.cache.anno.ExtendCacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @author liuzw
 * @date 2024/7/5
 */
@RestController
@RequestMapping("/api/example")
public class ExampleController {
  /**
   * 模拟向缓存中添加数据
   *
   * @return
   */
  @ExtendCacheable(value = "Example5", key = "#id", unless = "#result == null", expiredSecondTime = 200)
  @PostMapping("/add/{id}")
  public RestResult<Example> add(@PathVariable("id") Long id) {
    final Example example = new Example();
    example.setId(id);
    example.setDescribe(UUID.randomUUID().toString());
    example.setExampleTypeCode("code");
    System.out.println(example);
    return RestResult.buildSuccess(example);
  }
}
