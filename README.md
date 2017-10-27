# Private Beta Invitation Service

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7cf5461dde5149338d6029cabe2c6606)](https://www.codacy.com/app/HMCTS/private-beta-invitation-service)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/7cf5461dde5149338d6029cabe2c6606)](https://www.codacy.com/app/HMCTS/private-beta-invitation-service)
[![Build Status](https://travis-ci.org/hmcts/private-beta-invitation-service.svg?branch=master)](https://travis-ci.org/hmcts/private-beta-invitation-service)

This project is a service that sends private beta welcome emails to citizens. It constantly checks if there is
a new person that has agreed to join private beta and if there is one,
it calls [GOV.UK Notify](https://www.gov.uk/government/publications/govuk-notify)
to send the welcome email, using the appropriate email template.

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running quality checks

```bash
  ./gradlew check
```

### Checking test coverage

```bash
  ./gradlew jacocoTestReport
```

### Checking dependency vulnerabilities

```bash
  ./gradlew dependencyCheck
```

### Checking available updates for dependencies

```bash
  ./gradlew dependencyUpdates -Drevision=release
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew installDist
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/private-beta-invitation-service` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port.

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4700/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Running functional tests

Functional tests verify if the service is working as expected by talking to its dependencies -
 they feed the Azure Service Bus topic and verify if GOV.UK Notify has (or hasn't) sent
 appropriate emails. In order to test the right instance of the service you have to make sure
 that functional tests are talking to the right Azure Service Bus instance and feed the right topic.
 Here are the environment variables that you need to set for functional tests:

 - `TEST_NOTIFY_API_KEY` - test API key for GOV.UK Notify. This key is used by GOV.UK Notify
 client to authenticate. The client, in term, is used for retrieving information about emails
 that have been sent.
 - `TEST_SERVICE_BUS_CONNECTION_STRING` - connection string to Azure Service Bus,
 including the topic as entity path. This connection string is used for feeding the topic with
 test messages.
 - `TEST_SERVICE_BUS_POLLING_DELAY_MS` - the delay, in milliseconds, between consecutive
 Azure Service Bus message processing runs. This is for how long the service may not pick up
 messages from the subscription. Therefore, the tests need to know how long they should wait
 for results (i.e. emails sent) before they should fail.


Once you've got those variables set, you can run functional tests:
```bash
  ./gradlew functionalTest
```

Functional tests don't manage the service - they simply assume it's running. Therefore, if you
run tests locally, make sure you've started your instance (`./gradlew run-test-env`).

## Hystrix dashboard

When the service is running, you can monitor Hystrix metrics in real time using
[Hystrix Dashboard](https://github.com/Netflix/Hystrix/wiki/Dashboard).
In order to do this, visit `http://localhost:4700/hystrix` and provide `http://localhost:4700/hystrix.stream`
as the Hystrix event stream URL. Keep in mind that you'll only see data once some
of your Hystrix commands have been executed. Otherwise `Loading ...` message will be displayed
on the monitoring page.

## Configuration

### Using GOV.UK Notify email client stub

In order to use GOV.UK Notify email client stub, instead of the real client sending requests to Notify,
make sure you have `NOTIFY_USE_STUB` environment variable set to `true`.

### Configuring email template details for each service

In order to make this service send welcome emails to your service's private beta users,
you need to extend `emailTemplateMappings` section in [configuration](src/main/resources/application.yaml)
by adding an element containing `service`, `templateId`, `notifyApiKey` and `welcomeLink` properties:

```
emailTemplateMappings:
  - service: '...'      # The name of your service
    templateId: '...'   # ID of the email template in GOV.UK Notify
    notifyApiKey: '...' # Authentication key to use when calling Notify. Provide this value via environment variable, e.g. ${DIVORCE_NOTIFY_API_KEY}
    welcomeLink: '...'  # Welcome link to provide in the emails
  -
    ...
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
