package com.paykit.domain.customer.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class CustomerResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String billingAddress;
    private String gstin;
    private Instant createdAt;
    private Instant updatedAt;
}
