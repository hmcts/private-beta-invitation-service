package uk.gov.hmcts.reform.pbis;

import com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;
import uk.gov.hmcts.reform.pbis.model.EmailToSend;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.FIRST_NAME;
import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.LAST_NAME;
import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.WELCOME_LINK;

public class EmailCreator {

    private final Map<String, EmailTemplateMapping> emailTemplateMappings;

    public EmailCreator(List<EmailTemplateMapping> mappings) {
        this.emailTemplateMappings = mappings
            .stream()
            .collect(toMap(
                mapping -> mapping.getService(), Function.identity()
            ));
    }

    /**
     * Converts the given private beta registration into an object representing
     * email to be sent via notification service.
     *
     * @param registration Details of private beta registration
     * @return Complete information about the email to be sent
     */
    public EmailToSend createEmailToSend(final PrivateBetaRegistration registration) {

        assertTemplateConfiguredForService(registration.service);

        return new EmailToSend(
            registration.emailAddress,
            emailTemplateMappings.get(registration.service).getTemplateId(),
            ImmutableMap.of(
                FIRST_NAME, registration.firstName,
                LAST_NAME, registration.lastName,
                WELCOME_LINK, emailTemplateMappings.get(registration.service).getWelcomeLink()
            ),
            registration.referenceId
        );
    }

    private void assertTemplateConfiguredForService(final String service) {
        if (!emailTemplateMappings.containsKey(service)) {
            String errorMessage = String.format(
                "Service %s not found in email template configuration",
                service
            );

            throw new ServiceNotFoundException(errorMessage);
        }
    }
}
