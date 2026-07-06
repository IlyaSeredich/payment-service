package com.innowise.paymentservice.client;

import com.innowise.paymentservice.dto.RandomNumResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "random-number",
        url = "https://csrng.net/csrng/csrng.php"
)
public interface RandomNumFeignClient {
    @GetMapping
    RandomNumResponseDto getNum();
}
