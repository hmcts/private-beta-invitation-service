package uk.gov.hmcts.reform.pbis;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource(properties =
    {
        // prevent the application from doing actual work when context loads
        "scheduling.enable=false",
        "serviceBus.useStub=true",
        "notify.useStub=true"
    }
)
@SpringBootTest
public class ApplicationTest {

    @ClassRule
    public static EnvironmentVariables variables = new EnvironmentVariables();

    @BeforeClass
    public static void setUp() {
        variables.set("APPLICATION_INSIGHTS_IKEY", "some-key");
    }

    @Test
    public void contextLoads() {
    }

}
