package uk.gov.hmcts.reform.pbis;

public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException(String message) {
        super(message);
    }
}
