package uk.gov.hmcts.reform.pbis;

import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.FIRST_NAME;
import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.LAST_NAME;
import static uk.gov.hmcts.reform.pbis.model.TemplateFieldNames.WELCOME_LINK;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pbis.config.ApplicationConfig;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;
import uk.gov.hmcts.reform.pbis.model.EmailToSend;
import uk.gov.hmcts.reform.pbis.model.PrivateBetaRegistration;

public class EmailCreator {

    private final Map<String, EmailTemplateMapping> emailTemplateMappings;

    public EmailCreator(List<EmailTemplateMapping> mappings) {
        this.emailTemplateMappings = mappings
            .stream()
            .collect(Collectors.toMap(
                mapping -> mapping.getService(), Function.identity()
            ));
    }

    /**
     * Converts the given private beta registration into an object representing
     * email to be sent via notification service.
     * @param registration Details of private beta registration
     * @return Complete information about the email to be sent
     */
    public EmailToSend createEmailToSend(final PrivateBetaRegistration registration) {

        assertTemplateConfiguredForService(registration.service);

        return new EmailToSend(
            registration.emailAddress,
            emailTemplateMappings.get(registration.service).getTemplateId(),
            getFieldsForTemplate(registration),
            registration.referenceId
        );
    }

    private Map<String, String> getFieldsForTemplate(final PrivateBetaRegistration registration) {
        return ImmutableMap.<String, String>builder()
            .put(FIRST_NAME.getFieldName(), registration.firstName)
            .put(LAST_NAME.getFieldName(), registration.lastName)
            .put(
                WELCOME_LINK.getFieldName(),
                emailTemplateMappings.get(registration.service).getWelcomeLink()
            )
            .build();
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
