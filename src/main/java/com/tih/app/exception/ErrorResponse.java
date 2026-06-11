package com.tih.app.exception;

import lombok.Builder;

import java.util.List;

@Builder
public record ErrorResponse(String code, String message, String source, List<ValidationError> errors) {

}
