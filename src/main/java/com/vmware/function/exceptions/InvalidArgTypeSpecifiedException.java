package com.vmware.function.exceptions;

public class InvalidArgTypeSpecifiedException extends Exception {
    private static final String _VALID = "Invalid argument 1 must be ALL/REGION/GATEWAY/QUEUE/STORE";

    public InvalidArgTypeSpecifiedException(String message) {
        super(message + "\n" + _VALID);
    }
}
