package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.servicebus.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.logging.appinsights.AbstractAppInsights;
import uk.gov.hmcts.reform.pbis.MessageProcessingResult;
import uk.gov.hmcts.reform.pbis.MessageProcessingResultType;

import static uk.gov.hmcts.reform.pbis.telemetry.EventNames.EMAIL_SENT;
import static uk.gov.hmcts.reform.pbis.telemetry.EventNames.MESSAGE_PROCESSING_ERROR;
import static uk.gov.hmcts.reform.pbis.telemetry.EventNames.MESSAGE_PROCESSING_RUN_COMPLETED;
import static uk.gov.hmcts.reform.pbis.telemetry.EventNames.MESSAGE_PROCESSING_RUN_STARTED;
import static uk.gov.hmcts.reform.pbis.telemetry.EventNames.MESSAGE_REJECTED;
import static uk.gov.hmcts.reform.pbis.telemetry.MetricNames.FAILING_MESSAGES_PER_FUN;
import static uk.gov.hmcts.reform.pbis.telemetry.MetricNames.TOTAL_MESSAGES_PER_RUN;

/**
 * Logs events and sends telemetry data related with message queue processing.
 */
@Component
public class MessageQueueProcessingTracker extends AbstractAppInsights {

    private static final Logger logger =
        LoggerFactory.getLogger(MessageQueueProcessingTracker.class);

    private int totalMessageCount = 0;
    private int failingMessageCount = 0;

    public MessageQueueProcessingTracker(TelemetryClient telemetryClient) {
        super(telemetryClient);
    }

    public void trackProcessingStarted() {
        logger.info("Processing messages from subscription queue.");
        telemetry.trackEvent(MESSAGE_PROCESSING_RUN_STARTED);
        totalMessageCount = 0;
        failingMessageCount = 0;
    }

    public void trackProcessingCompleted() {
        sendTelemetryDataForCompletedRun(totalMessageCount, failingMessageCount);

        logger.info(
            String.format(
                "No more messages to process. Total: %s, failed: %s.",
                totalMessageCount,
                failingMessageCount
            )
        );
    }

    public void trackProcessingError(Exception exception) {
        logger.error(
            "An error occurred when processing messages from subscription queue.",
            exception
        );
    }

    public void trackReceivedMessage(String id) {
        logger.info(String.format("Received message with ID %s.", id));
    }

    public void trackMessageProcessingResult(
        MessageProcessingResult processingResult,
        IMessage message
    ) {
        logProcessingResult(processingResult, message);
        sendTelemetryDataForMessage(processingResult);

        if (processingResult.resultType != MessageProcessingResultType.SUCCESS) {
            failingMessageCount++;
        }

        totalMessageCount++;
    }

    private void logProcessingResult(
        MessageProcessingResult processingResult,
        IMessage message
    ) {
        switch (processingResult.resultType) {
            case SUCCESS:
                logMessageProcessingSuccess(message);
                break;
            case ERROR:
                logMessageProcessingFailure(processingResult.errorDetails, message);
                break;
            case UNPROCESSABLE_MESSAGE:
                logUnprocessableMessage(processingResult.errorDetails, message);
                break;
            default:
        }
    }

    private void sendTelemetryDataForMessage(MessageProcessingResult processingResult) {
        switch (processingResult.resultType) {
            case SUCCESS:
                telemetry.trackEvent(EMAIL_SENT);
                break;
            case ERROR:
                telemetry.trackEvent(MESSAGE_PROCESSING_ERROR);
                break;
            case UNPROCESSABLE_MESSAGE:
                telemetry.trackEvent(MESSAGE_REJECTED);
                break;
            default:
                logger.warn("Unknown processing result type: {}", processingResult.resultType);
        }
    }

    private void sendTelemetryDataForCompletedRun(int messageCount, int failureCount) {
        telemetry.trackEvent(MESSAGE_PROCESSING_RUN_COMPLETED);
        telemetry.trackMetric(TOTAL_MESSAGES_PER_RUN, messageCount);
        telemetry.trackMetric(FAILING_MESSAGES_PER_FUN, failureCount);
    }

    private void logMessageProcessingSuccess(IMessage message) {
        logger.info(String.format(
            "Completed processing message with ID %s on attempt %s.",
            message.getMessageId(),
            message.getDeliveryCount() + 1)
        );
    }

    private void logUnprocessableMessage(
        MessageProcessingResult.ProcessingError processingError,
        IMessage message
    ) {
        String invalidFieldDetails = processingError.fieldValidationErrors != null
            ? " (" + processingError.fieldValidationErrors + ")"
            : "";

        logger.warn(
            String.format(
                "Rejected message with ID %s. Reason: %s - %s%s",
                message.getMessageId(),
                processingError.reason,
                processingError.description,
                invalidFieldDetails
            )
        );
    }

    private void logMessageProcessingFailure(
        MessageProcessingResult.ProcessingError processingError,
        IMessage message
    ) {
        logger.error(
            String.format(
                "Failed to process message with ID %s on attempt %s.",
                message.getMessageId(),
                message.getDeliveryCount() + 1
            ),
            processingError.exception
        );
    }
}
