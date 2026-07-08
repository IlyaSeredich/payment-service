package com.innowise.paymentservice.exception;

public class LiquibaseInitException extends RuntimeException{
    public LiquibaseInitException(String message) {
        super(message);
    }
}
