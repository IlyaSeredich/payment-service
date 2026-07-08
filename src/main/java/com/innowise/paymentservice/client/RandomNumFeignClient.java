package com.innowise.paymentservice.client;

import com.innowise.paymentservice.dto.RandomNumResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "random-num-client",
        url = "${random.num.url}"
)
public interface RandomNumFeignClient {
    @GetMapping
    List<RandomNumResponseDto> getNum();
}
