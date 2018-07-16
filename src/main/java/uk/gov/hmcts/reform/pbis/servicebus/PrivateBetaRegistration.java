package uk.gov.hmcts.reform.pbis.servicebus;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

public final class PrivateBetaRegistration {

    @NotEmpty
    public final String referenceId;

    @NotEmpty
    public final String service;

    @NotEmpty
    @Email
    public final String emailAddress;

    @NotEmpty
    public final String firstName;

    @NotEmpty
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
