package uk.gov.hmcts.reform.pbis.servicebus;

public class ServiceBusException extends RuntimeException {

    public ServiceBusException(String message, Throwable cause) {
        super(message, cause);
    }
}
