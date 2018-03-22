package uk.gov.hmcts.reform.pbis;

import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

/**
 * SonarQube reports as error. Max allowed - 5 parents
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ServiceNotFoundException extends UnknownErrorCodeException {

    public ServiceNotFoundException(String message) {
        super(AlertLevel.P3, message);
    }
}
