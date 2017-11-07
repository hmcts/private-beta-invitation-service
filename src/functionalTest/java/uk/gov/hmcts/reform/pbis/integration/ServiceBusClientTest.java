package uk.gov.hmcts.reform.pbis.integration;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.azure.servicebus.IMessage;import java.util.List;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.pbis.categories.IntegrationTests;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Category(IntegrationTests.class)
public class ServiceBusClientTest extends AbstractServiceBusTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void receiveMessage_should_receive_next_message_from_subscription() throws Exception {
        String[] sentMessageContents = {"message1", "message2", "message3"};

        for (String content : sentMessageContents) {
            serviceBusFeeder.sendMessage(content);
        }

        List<IMessage> receivedMessages = Stream
            .generate(() -> receiveMessage())
            .limit(sentMessageContents.length)
            .collect(toList());

        assertThat(receivedMessages)
            .as("check if received messages contain no nulls")
            .doesNotContainNull();

        assertThat(contents(receivedMessages))
            .as("check contents of received messages")
            .containsExactlyInAnyOrder(sentMessageContents);
    }

    @Test
    public void completeMessage_should_remove_message_from_the_queue() throws Exception {
        serviceBusFeeder.sendMessage("content1");
        serviceBusFeeder.sendMessage("content2");

        IMessage completed = receiveMessage();
        IMessage notCompleted = receiveMessage();

        assertThat(completed).as("check if received message is not null").isNotNull();
        assertThat(notCompleted).as("check if received message is not null").isNotNull();
        assertThat(receiveMessage()).as("check if subscription is empty").isNull();

        completeMessage(completed);

        // wait for message locks to expire
        Thread.sleep(testConfig.getMessageLockTimeoutInMs());

        // verify that the only message in subscription is the not completed one
        assertSameMessage(receiveMessage(), notCompleted);
        assertThat(receiveMessage()).as("check if subscription is empty").isNull();
    }

    private void assertSameMessage(IMessage actual, IMessage expected) {
        assertThat(actual).as("check if message is not empty").isNotNull();

        assertThat(bodyAsString(actual))
            .as("check message body")
            .isEqualTo(bodyAsString(expected));

        assertThat(actual.getMessageId())
            .as("check message ID")
            .isEqualTo(expected.getMessageId());
    }

    private List<String> contents(List<IMessage> messages) {
        return messages
            .stream()
            .map(message -> bodyAsString(message))
            .collect(toList());
    }

    private String bodyAsString(IMessage message) {
        return new String(message.getBody());
    }

    private void completeMessage(IMessage message) {
        serviceBusClient.completeMessage(message.getMessageId(), message.getLockToken());
        messagesToComplete.remove(message.getMessageId());
    }
}
