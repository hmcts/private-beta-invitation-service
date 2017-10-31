#!/usr/bin/env sh

# Runs dev environment, which talks to real GOV.UK Notify and Azure Service Bus service instances.

export SPRING_PROFILES_ACTIVE=dev

./gradlew clean installDist && docker-compose build && docker-compose up
