package com.tih.app.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ConflictResolution(@NotNull(message = "extId is required") UUID extId, @NotNull(message = "action is required") String action) {

}
