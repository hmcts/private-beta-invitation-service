package uk.gov.hmcts.reform.pbis.notify;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pbis.ServiceNotFoundException;
import uk.gov.hmcts.reform.pbis.config.ApplicationConfig;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientApi;

@Component
public class NotificationClientProvider {

    private final Map<String, NotificationClientApi> notificationClientsByService;

    @Autowired
    public NotificationClientProvider(ApplicationConfig config) {
        notificationClientsByService = getApiClientsByService(
            config.getEmailTemplateMappings(),
            config.getUseNotifyClientStub()
        );
    }

    public NotificationClientApi getClient(String service) {
        if (notificationClientsByService.containsKey(service)) {
            return notificationClientsByService.get(service);
        } else {
            throw new ServiceNotFoundException(
                String.format("Failed to find Notify API client for service %s", service)
            );
        }
    }

    private Map<String, NotificationClientApi> getApiClientsByService(
        List<EmailTemplateMapping> emailTemplateMappings,
        boolean useStub
    ) {
        return emailTemplateMappings
            .stream()
            .collect(
                Collectors.toMap(
                    mapping -> mapping.getService(),
                    mapping -> getNotificationClient(mapping.getNotifyApiKey(), useStub)
                )
            );
    }

    private NotificationClientApi getNotificationClient(String notifyApiKey, boolean useStub) {
        if (useStub) {
            return new NotificationClientStub();
        } else {
            return new NotificationClient(notifyApiKey);
        }
    }
}