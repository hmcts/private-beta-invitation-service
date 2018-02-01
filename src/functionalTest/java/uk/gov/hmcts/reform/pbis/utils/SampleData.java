package uk.gov.hmcts.reform.pbis.utils;

import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class SampleData {

    private SampleData() {
        // utility class constructor
    }

    public static List<PrivateBetaRegistration> getSampleRegistrations(
        String service,
        int numberOfRegistrations
    ) {
        return Stream
            .generate(() -> SampleData.getSampleRegistration(service))
            .limit(numberOfRegistrations)
            .collect(toList());
    }

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

    public static PrivateBetaRegistration getSampleInvalidRegistration() {
        String reference = "pbis-test-" + UUID.randomUUID().toString();
        return new PrivateBetaRegistration(reference, "", "not-an-email", "", "");
    }
}
