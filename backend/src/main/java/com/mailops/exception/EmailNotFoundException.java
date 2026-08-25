package com.mailops.exception;

public class EmailNotFoundException extends RuntimeException {
    public EmailNotFoundException(Long emailId) {
        super("Email not found with id: " + emailId);
    }
}
