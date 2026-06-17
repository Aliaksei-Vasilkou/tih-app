package com.tih.app.dto;

import java.util.List;

public record BatchAnalyseResponse(List<QuestionTransferItem> newItems, List<DuplicateConflict> duplicates) {

}
