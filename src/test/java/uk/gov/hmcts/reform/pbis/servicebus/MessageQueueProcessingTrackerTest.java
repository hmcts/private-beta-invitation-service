package uk.gov.hmcts.reform.pbis.servicebus;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.servicebus.IMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.pbis.MessageProcessingResult;
import uk.gov.hmcts.reform.pbis.MessageProcessingResultType;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResultType.ERROR;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResultType.SUCCESS;
import static uk.gov.hmcts.reform.pbis.MessageProcessingResultType.UNPROCESSABLE_MESSAGE;

@RunWith(MockitoJUnitRunner.class)
public class MessageQueueProcessingTrackerTest {

    @Mock
    private TelemetryClient telemetryClient;

    private MessageQueueProcessingTracker tracker;

    @Before
    public void setUp() {
        tracker = new MessageQueueProcessingTracker(telemetryClient);
    }

    @Test
    public void trackProcessingStarted_sends_the_right_event() {
        tracker.trackProcessingStarted();

        verify(telemetryClient).trackEvent("MessageProcessingRunStarted");
        verifyNoMoreInteractions(telemetryClient);
    }

    @Test
    public void trackProcessingCompleted_sends_the_right_event_and_metrics() {
        int successCount = 1;
        int errorCount = 2;
        int rejectionCount = 3;

        trackMessageProcessingResults(successCount, SUCCESS);
        trackMessageProcessingResults(errorCount, ERROR);
        trackMessageProcessingResults(rejectionCount, UNPROCESSABLE_MESSAGE);

        tracker.trackProcessingCompleted();

        verify(telemetryClient).trackEvent("MessageProcessingRunCompleted");

        int totalCount = successCount + errorCount + rejectionCount;
        verify(telemetryClient).trackMetric("TotalMessagesPerRun", totalCount);

        int failureCount = errorCount + rejectionCount;
        verify(telemetryClient).trackMetric("FailingMessagesPerRun", failureCount);
    }

    @Test
    public void trackProcessingError_does_not_throw_exception() {
        assertThatCode(
            () -> tracker.trackProcessingError(new RuntimeException("test"))
        ).doesNotThrowAnyException();
    }

    @Test
    public void trackReceivedMessage__does_not_throw_exception() {
        assertThatCode(
            () -> tracker.trackReceivedMessage("id")
        ).doesNotThrowAnyException();
    }

    @Test
    public void trackMessageProcessingResult_sends_success_event_for_success() {
        tracker.trackMessageProcessingResult(
            new MessageProcessingResult(SUCCESS, null),
            mock(IMessage.class)
        );

        verify(telemetryClient).trackEvent("EmailSent");
        verifyNoMoreInteractions(telemetryClient);
    }

    @Test
    public void trackMessageProcessingResult_sends_error_event_for_error() {
        tracker.trackMessageProcessingResult(
            createProcessingResult(ERROR, true),
            mock(IMessage.class)
        );

        verify(telemetryClient).trackEvent("MessageProcessingError");
        verifyNoMoreInteractions(telemetryClient);
    }

    @Test
    public void trackMessageProcessingResult_sends_rejected_event_for_rejected_message() {
        tracker.trackMessageProcessingResult(
            createProcessingResult(UNPROCESSABLE_MESSAGE, true),
            mock(IMessage.class)
        );

        verify(telemetryClient).trackEvent("MessageRejected");
        verifyNoMoreInteractions(telemetryClient);
    }

    private void trackMessageProcessingResults(
        int messageCount,
        MessageProcessingResultType resultType
    ) {
        for (int i = 0; i < messageCount; i++) {
            tracker.trackMessageProcessingResult(
                createProcessingResult(resultType, resultType != SUCCESS),
                mock(IMessage.class)
            );
        }
    }

    private MessageProcessingResult createProcessingResult(
        MessageProcessingResultType resultType,
        boolean addError
    ) {
        MessageProcessingResult.ProcessingError error = null;

        if (addError) {
            error = new MessageProcessingResult.ProcessingError(
                "reason",
                "description",
                null,
                null
            );
        }

        return new MessageProcessingResult(resultType, error);
    }
}
