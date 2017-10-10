# Private Beta Invitation Service

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

## Hystrix dashboard

When the service is running, you can monitor Hystrix metrics in real time using
[Hystrix Dashboard](https://github.com/Netflix/Hystrix/wiki/Dashboard).
In order to do this, visit `http://localhost:4700/hystrix` and provide `http://localhost:4700/hystrix.stream`
as the Hystrix event stream URL. Keep in mind that you'll only see data once some
of your Hystrix commands have been executed. Otherwise `Loading ...` message will be displayed
on the monitoring page.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
