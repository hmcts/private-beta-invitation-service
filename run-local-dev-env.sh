#!/usr/bin/env sh

# Runs local-dev environment, which doesn't talk to external dependencies
# (uses client stubs for GOV.UK Notify and Azure Service Bus instead)

export SPRING_PROFILES_ACTIVE=local-dev

./gradlew clean installDist && docker-compose build && docker-compose up
