package au.com.customer.core.internal.exception;

public enum ErrorCode {

    /**
     * Redirect Failed.
     */
    FAILED_REDIRECT(5000, "Failed to redirect to success page"),

    /**
     * Redirect not configured.
     */
    REDIRECT_NOT_CONFIGURED(5001, "Redirect target not configured in the form container"),

    /**
     * Request dispatching failed.
     */
    DISPATCH_FAILED(5002, "Can't get request dispatcher to forward the response"),

    /**
     * Resolver not found.
     */
    RESOLVER_NOT_FOUND(5003, "Unable to get resource resolver or permissions missing, check permissions of system user");

    /**
     * Error code.
     */
    private final int code;

    /**
     * Error message.
     */
    private final String msg;

    ErrorCode(final int code, final String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }
}
