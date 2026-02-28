package com.bikeexchange.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ListingNotAvailableException extends RuntimeException {
    public ListingNotAvailableException(String message) {
        super(message);
    }
}
