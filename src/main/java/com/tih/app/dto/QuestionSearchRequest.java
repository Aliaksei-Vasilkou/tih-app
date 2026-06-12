package com.tih.app.dto;

public record QuestionSearchRequest(String query, Long languageId, Long categoryId, String tag, int page, int size) {

}
