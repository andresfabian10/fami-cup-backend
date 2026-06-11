package com.famicup.modelo.validacion;

import com.famicup.excepcion.ReglaNegocioException;

public final class PasswordPolicy {

    public static final int MIN_PASSWORD_LENGTH = 3;
    public static final int MAX_PASSWORD_LENGTH = 80;
    public static final String MIN_LENGTH_MESSAGE = "La contraseña debe tener al menos 3 caracteres.";
    public static final String MAX_LENGTH_MESSAGE = "La contraseña no puede superar 80 caracteres.";
    public static final String REQUIRED_MESSAGE = "La contraseña es obligatoria.";

    private PasswordPolicy() {
    }

    public static String normalize(String password) {
        return password == null ? "" : password.trim();
    }

    public static String normalizeAndValidate(String password) {
        String normalizedPassword = normalize(password);

        if (normalizedPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ReglaNegocioException(MIN_LENGTH_MESSAGE);
        }
        if (normalizedPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new ReglaNegocioException(MAX_LENGTH_MESSAGE);
        }

        return normalizedPassword;
    }

    public static String normalizeOptionalAndValidate(String password) {
        String normalizedPassword = normalize(password);

        if (normalizedPassword.isBlank()) {
            return null;
        }

        return normalizeAndValidate(normalizedPassword);
    }
}
