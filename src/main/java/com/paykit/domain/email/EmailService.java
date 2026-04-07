package com.paykit.domain.email;

public interface EmailService {

    void sendInvoiceEmail(String to, String subject, String body);
}
