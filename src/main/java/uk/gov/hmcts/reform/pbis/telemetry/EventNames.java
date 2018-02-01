package uk.gov.hmcts.reform.pbis.telemetry;

public class EventNames {

    private EventNames() {
        // utility class constructor
    }

    public static final String MESSAGE_PROCESSING_RUN_STARTED = "MessageProcessingRunStarted";
    public static final String MESSAGE_PROCESSING_RUN_COMPLETED = "MessageProcessingRunCompleted";
    public static final String EMAIL_SENT = "EmailSent";
    public static final String MESSAGE_PROCESSING_ERROR = "MessageProcessingError";
    public static final String MESSAGE_REJECTED = "MessageRejected";
}
