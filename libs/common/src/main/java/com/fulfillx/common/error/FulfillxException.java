package com.fulfillx.common.error;

public class FulfillxException extends RuntimeException {
    private final ErrorCode code;

    public FulfillxException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public FulfillxException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
