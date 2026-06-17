package com.tih.app.dto;

import java.util.List;

public record BatchCommitRequest(List<QuestionTransferItem> newItems, List<ConflictResolution> resolutions) {

}
