package com.tih.app.dto;

public record QuestionSearchRequest(String query, Long languageId, Long categoryId, int page, int size) {

}
