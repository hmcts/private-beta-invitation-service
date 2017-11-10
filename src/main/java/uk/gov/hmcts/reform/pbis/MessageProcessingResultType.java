package uk.gov.hmcts.reform.pbis;

public enum MessageProcessingResultType {
    // Message processed successfully
    SUCCESS,

    // Message processing failed because of an error
    ERROR,

    // Message processing failed because the message is unprocessable,
    // either because it has invalid format or invalid data
    UNPROCESSABLE_MESSAGE,
}
