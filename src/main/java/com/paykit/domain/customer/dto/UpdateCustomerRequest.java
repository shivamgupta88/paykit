package com.paykit.domain.customer.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerRequest {

    private String name;

    @Email
    private String email;

    private String phone;

    private String billingAddress;

    private String gstin;
}
