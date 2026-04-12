package com.umbarry.usermanagementservice.service;

public interface MailService {
    void sendWelcomeEmail(String to, String username, String password);
}
