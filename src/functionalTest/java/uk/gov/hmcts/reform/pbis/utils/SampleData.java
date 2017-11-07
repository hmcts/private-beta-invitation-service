package uk.gov.hmcts.reform.pbis.utils;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;


public class SampleData {

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
}
