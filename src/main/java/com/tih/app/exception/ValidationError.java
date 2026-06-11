package com.tih.app.exception;

public record ValidationError(String code, String field, String message) {

}
