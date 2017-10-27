#!/usr/bin/env sh

export SPRING_PROFILES_ACTIVE=test

./gradlew clean installDist && docker-compose build && docker-compose up
