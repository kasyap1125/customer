package au.com.customer.core.internal.exception;

/**
 * Exception class to handle various errors for form submission.
 */
public class SubmissionFailureException extends RuntimeException {

    /**
     * Error code.
     */
    private final int errorCode;

    /**
     * Error message.
     */
    private final String errorMessage;

    /**
     * Constructor with error code.
     *
     * @param errorCode Error code with code and message.
     */
    public SubmissionFailureException(final ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMsg();
    }

    /**
     * Constructor with error code, message and cause.
     *
     * @param cause Throwable cause.
     * @param errorCode error code with code and message.
     */
    public SubmissionFailureException(final Throwable cause,
                                      final ErrorCode errorCode) {
        super(errorCode.getMsg(), cause);
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMsg();
    }

}
