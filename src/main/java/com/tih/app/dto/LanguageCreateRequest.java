package com.tih.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LanguageCreateRequest(

        @NotBlank(message = "Language name is required")
        @Size(max = 100, message = "Language name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Language code is required")
        @Size(max = 50, message = "Language code must not exceed 50 characters")
        String code) {

}
