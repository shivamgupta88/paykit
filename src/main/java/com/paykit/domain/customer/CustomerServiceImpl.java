package com.paykit.domain.customer;

import com.paykit.domain.customer.dto.CreateCustomerRequest;
import com.paykit.domain.customer.dto.CustomerResponse;
import com.paykit.domain.customer.dto.UpdateCustomerRequest;
import com.paykit.exception.ResourceNotFoundException;
import com.paykit.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        Customer customer = customerMapper.toEntity(request);
        customer = customerRepository.save(customer);
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        Customer customer = findByIdAndTenant(id);
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse update(UUID id, UpdateCustomerRequest request) {
        Customer customer = findByIdAndTenant(id);
        customerMapper.updateEntity(request, customer);
        customer = customerRepository.save(customer);
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Customer customer = findByIdAndTenant(id);
        customerRepository.delete(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return customerRepository.findAllByTenantId(tenantId, pageable)
                .map(customerMapper::toResponse);
    }

    private Customer findByIdAndTenant(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        if (!customer.getTenantId().equals(TenantContext.get())) {
            throw new ResourceNotFoundException("Customer", id);
        }
        return customer;
    }
}
