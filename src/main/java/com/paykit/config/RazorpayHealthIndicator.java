package com.paykit.config;

import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RazorpayHealthIndicator implements HealthIndicator {

    private final RazorpayClient razorpayClient;

    @Override
    public Health health() {
        try {
            razorpayClient.orders.fetchAll();
            return Health.up().withDetail("razorpay", "reachable").build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("razorpay", "unreachable")
                    .withException(e)
                    .build();
        }
    }
}
