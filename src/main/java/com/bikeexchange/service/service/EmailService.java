package com.bikeexchange.service.service;

import com.bikeexchange.model.User;

public interface EmailService {
    void sendVerificationEmail(User user, String token);
    void sendResetPasswordEmail(User user, String token);
}
