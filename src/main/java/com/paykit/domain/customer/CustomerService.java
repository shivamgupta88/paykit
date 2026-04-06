package com.paykit.domain.customer;

import com.paykit.domain.customer.dto.CreateCustomerRequest;
import com.paykit.domain.customer.dto.CustomerResponse;
import com.paykit.domain.customer.dto.UpdateCustomerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerResponse create(CreateCustomerRequest request);

    CustomerResponse getById(UUID id);

    CustomerResponse update(UUID id, UpdateCustomerRequest request);

    void delete(UUID id);

    Page<CustomerResponse> list(Pageable pageable);
}
