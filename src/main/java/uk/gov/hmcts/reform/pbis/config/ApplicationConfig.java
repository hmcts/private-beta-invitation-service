package uk.gov.hmcts.reform.pbis.config;

import com.microsoft.applicationinsights.TelemetryClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pbis.EmailCreator;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;
import uk.gov.hmcts.reform.pbis.notify.NotificationClientProvider;
import uk.gov.hmcts.reform.pbis.servicebus.IServiceBusClientFactory;
import uk.gov.hmcts.reform.pbis.servicebus.ServiceBusClientFactory;
import uk.gov.hmcts.reform.pbis.servicebus.ServiceBusClientStub;


@Configuration
@ConfigurationProperties
public class ApplicationConfig {

    @Value("${notify.useStub}")
    private boolean useNotifyClientStub;

    @Value("${serviceBus.connectionString}")
    private String serviceBusConnectionString;

    @Value("${serviceBus.maxReceiveWaitTimeInMs}")
    private long maxReceiveWaitTimeMs;

    private final List<EmailTemplateMapping> emailTemplateMappings = new ArrayList<>();

    // this getter is needed by the framework
    public List<EmailTemplateMapping> getEmailTemplateMappings() {
        return emailTemplateMappings;
    }

    @Bean
    public EmailCreator getEmailCreator() {
        return new EmailCreator(emailTemplateMappings);
    }

    @Bean
    public NotificationClientProvider getNotificationClientProvider() {
        return new NotificationClientProvider(emailTemplateMappings, useNotifyClientStub);
    }

    @Bean
    @ConditionalOnProperty(name = "serviceBus.useStub", havingValue = "false")
    public IServiceBusClientFactory getServiceBusClientFactory() {
        return new ServiceBusClientFactory(
            serviceBusConnectionString,
            Duration.ofMillis(maxReceiveWaitTimeMs)
        );
    }

    @Bean
    @ConditionalOnProperty(name = "serviceBus.useStub", havingValue = "true")
    public IServiceBusClientFactory getServiceBusClientStubFactory() {
        return () -> ServiceBusClientStub.getInstance();
    }

    @Bean
    public Validator getValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }

    @Bean
    public TelemetryClient getTelemetryClient() {
        return new TelemetryClient();
    }
}
