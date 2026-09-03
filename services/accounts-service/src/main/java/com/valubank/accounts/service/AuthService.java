package com.valubank.accounts.service;

import com.valubank.accounts.dto.LoginRequest;
import com.valubank.accounts.dto.LoginResponse;
import com.valubank.accounts.entity.Customer;
import com.valubank.accounts.exception.InvalidCredentialsException;
import com.valubank.accounts.repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;

    public AuthService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public LoginResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        // Deliberately simplified for the workshop: plaintext password comparison, no JWT/session tokens.
        if (!customer.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return new LoginResponse(customer.getId(), customer.getUsername(), customer.getFullName(), customer.isAdmin());
    }
}
