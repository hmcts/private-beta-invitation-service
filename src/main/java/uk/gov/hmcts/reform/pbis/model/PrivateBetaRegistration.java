package uk.gov.hmcts.reform.pbis.model;

public final class PrivateBetaRegistration {

    public final String referenceId;
    public final String service;
    public final String emailAddress;
    public final String firstName;
    public final String lastName;


    public PrivateBetaRegistration(
        final String referenceId,
        final String service,
        final String emailAddress,
        final String firstName,
        final String lastName
    ) {
        this.referenceId = referenceId;
        this.service = service;
        this.emailAddress = emailAddress;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
