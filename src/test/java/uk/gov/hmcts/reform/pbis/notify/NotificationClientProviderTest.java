package uk.gov.hmcts.reform.pbis.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.pbis.ServiceNotFoundException;
import uk.gov.hmcts.reform.pbis.config.ApplicationConfig;
import uk.gov.hmcts.reform.pbis.model.EmailTemplateMapping;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientApi;


public class NotificationClientProviderTest {

    private static final String SERVICE_1_NAME = "service1";
    private static final String SERVICE_2_NAME = "service2";

    @Test
    public void getClient_should_return_stub_when_stub_flag_is_on() {
        NotificationClientProvider clientProvider = createClientProvider(true);

        NotificationClientApi client = clientProvider.getClient(SERVICE_1_NAME);

        assertThat(client).isExactlyInstanceOf(NotificationClientStub.class);
    }

    @Test
    public void getClient_should_return_api_client_when_stub_flag_is_off() {
        NotificationClientProvider clientProvider = createClientProvider(false);

        NotificationClientApi client = clientProvider.getClient(SERVICE_1_NAME);

        assertThat(client).isInstanceOf(NotificationClient.class);
    }

    @Test
    public void getClient_should_return_different_clients_for_different_services() {
        NotificationClientProvider clientProvider = createClientProvider(false);

        NotificationClientApi clientForService1 = clientProvider.getClient(SERVICE_1_NAME);
        NotificationClientApi clientForService2 = clientProvider.getClient(SERVICE_2_NAME);
        assertThat(clientForService2).isNotSameAs(clientForService1);
    }

    @Test
    public void getClient_should_return_same_client_for_same_service_each_time() {
        NotificationClientProvider clientProvider = createClientProvider(false);

        NotificationClientApi client1 = clientProvider.getClient(SERVICE_1_NAME);
        NotificationClientApi client2 = clientProvider.getClient(SERVICE_1_NAME);
        assertThat(client2).isSameAs(client1);
    }

    @Test(expected = ServiceNotFoundException.class)
    public void getClient_should_throw_exception_when_service_not_configured() {
        createClientProvider(false).getClient("unknown-service-name");
    }

    private NotificationClientProvider createClientProvider(boolean useClientStub) {
        List<EmailTemplateMapping> emailTemplateMappings = Arrays.asList(
            createTemplateMapping(SERVICE_1_NAME, "apiKey1"),
            createTemplateMapping(SERVICE_2_NAME, "apiKey2")
        );

        ApplicationConfig config = mock(ApplicationConfig.class);
        when(config.getEmailTemplateMappings()).thenReturn(emailTemplateMappings);

        when(config.getUseNotifyClientStub()).thenReturn(useClientStub);
        return new NotificationClientProvider(config);
    }

    private static EmailTemplateMapping createTemplateMapping(String service, String apiKey) {
        EmailTemplateMapping mapping = new EmailTemplateMapping();
        mapping.setService(service);
        mapping.setNotifyApiKey(apiKey);
        return mapping;
    }
}
