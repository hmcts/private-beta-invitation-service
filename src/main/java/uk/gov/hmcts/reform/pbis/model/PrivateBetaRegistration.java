package uk.gov.hmcts.reform.pbis.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class PrivateBetaRegistration {

    public final String referenceId;
    public final String service;
    public final String emailAddress;
    public final String firstName;
    public final String lastName;


    public PrivateBetaRegistration(
        @JsonProperty("reference_id") final String referenceId,
        @JsonProperty("service") final String service,
        @JsonProperty("email_address") final String emailAddress,
        @JsonProperty("first_name") final String firstName,
        @JsonProperty("last_name") final String lastName
    ) {
        this.referenceId = referenceId;
        this.service = service;
        this.emailAddress = emailAddress;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
