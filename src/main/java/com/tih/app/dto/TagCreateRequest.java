package com.tih.app.dto;

import jakarta.validation.constraints.NotBlank;

public record TagCreateRequest(

        @NotBlank(message = "Tag name is required")
        String name) {

}
