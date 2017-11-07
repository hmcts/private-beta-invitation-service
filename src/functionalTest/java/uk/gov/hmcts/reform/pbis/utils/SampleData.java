package uk.gov.hmcts.reform.pbis.utils;

import java.util.UUID;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;


public class SampleData {

    public static PrivateBetaRegistration getSampleRegistration(String service) {
        String reference = "pbis-test-" + UUID.randomUUID().toString();

        return new PrivateBetaRegistration(
            reference,
            service,
            "john.smith@example.com",
            "John",
            "Smith"
        );
    }
}
