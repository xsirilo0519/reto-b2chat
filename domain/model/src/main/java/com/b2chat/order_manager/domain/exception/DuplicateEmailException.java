package com.b2chat.order_manager.domain.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("El usuario ya está registrado: " + email);
    }
}

