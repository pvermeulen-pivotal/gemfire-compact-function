package com.vmware.gemfire.function.exceptions;

public class InvalidArgumentTypeException extends Exception {
    private static final String _VALID = "Invalid argument 1 must be ALL/REGION/GATEWAY/QUEUE/STORE";

    public InvalidArgumentTypeException(String message) {
        super(message + "\n" + _VALID);
    }
}
