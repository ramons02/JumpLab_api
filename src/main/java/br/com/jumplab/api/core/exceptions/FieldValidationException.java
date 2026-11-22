package br.com.jumplab.api.core.exceptions;

public class FieldValidationException extends BaseException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
