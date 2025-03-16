package com.movieflix.movieApi.exceptions;

public class FileExistsException extends RuntimeException {
    public FileExistsException(String message) {
        super(message);
    }
}
