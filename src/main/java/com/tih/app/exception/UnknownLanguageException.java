package com.tih.app.exception;

public class UnknownLanguageException extends RuntimeException {

    public UnknownLanguageException(String languageCode) {
        super("Language not found with code: " + languageCode);
    }
}
