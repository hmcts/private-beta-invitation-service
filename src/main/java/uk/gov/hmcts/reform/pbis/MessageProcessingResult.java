package uk.gov.hmcts.reform.pbis;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;


public class MessageProcessingResult {

    public final MessageProcessingResultType resultType;
    public final ProcessingError errorDetails;

    public MessageProcessingResult(
        MessageProcessingResultType resultType,
        ProcessingError error
    ) {
        this.resultType = resultType;
        this.errorDetails = error;
    }

    public static MessageProcessingResult success() {
        return new MessageProcessingResult(MessageProcessingResultType.SUCCESS, null);
    }

    public static MessageProcessingResult invalidMessageFormat() {
        return new MessageProcessingResult(
            MessageProcessingResultType.UNPROCESSABLE_MESSAGE,
            new MessageProcessingResult.ProcessingError(
                "Invalid message",
                "Message body has invalid format",
                null,
                null
            )
        );
    }

    public static MessageProcessingResult invalidMessageData(
        Set<ConstraintViolation<PrivateBetaRegistration>> violations
    ) {
        return new MessageProcessingResult(
            MessageProcessingResultType.UNPROCESSABLE_MESSAGE,
            new MessageProcessingResult.ProcessingError(
                "Invalid message",
                "Message contains invalid data",
                getValidationErrorMap(violations),
                null
            )
        );
    }

    public static MessageProcessingResult processingError(Exception cause) {
        return new MessageProcessingResult(
            MessageProcessingResultType.ERROR,
            new MessageProcessingResult.ProcessingError(null, null, null, cause)
        );
    }

    private static Map<String, String> getValidationErrorMap(
        Set<ConstraintViolation<PrivateBetaRegistration>> violations
    ) {
        return violations
            .stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                violation -> violation.getMessage())
            );
    }

    public static class ProcessingError {
        public final String reason;
        public final String description;
        public final Map<String, String> fieldValidationErrors;
        public final Exception exception;

        public ProcessingError(
            String reason,
            String description,
            Map<String, String> fieldValidationErrors,
            Exception exception
        ) {
            this.reason = reason;
            this.description = description;
            this.fieldValidationErrors = fieldValidationErrors;
            this.exception = exception;
        }
    }
}
