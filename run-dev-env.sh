#!/usr/bin/env sh

export SPRING_PROFILES_ACTIVE=dev

./gradlew clean installDist && docker-compose build && docker-compose up
