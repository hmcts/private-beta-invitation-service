package uk.gov.hmcts.reform.pbis;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class EmailSendingException extends UnknownErrorCodeException {

    public EmailSendingException(String message, Throwable cause) {
        super(AlertLevel.P1, message, cause);
    }
}
