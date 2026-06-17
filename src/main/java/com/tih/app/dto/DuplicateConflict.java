package com.tih.app.dto;

import java.util.UUID;

public record DuplicateConflict(UUID extId, QuestionDto existing, QuestionTransferItem incoming) {

}
