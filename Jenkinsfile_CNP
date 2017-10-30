#!groovy
properties(
  [
    [
      $class: 'GithubProjectProperty',
      projectUrlStr: 'https://github.com/hmcts/private-beta-invitation-service'
    ],
    pipelineTriggers([[$class: 'GitHubPushTrigger']])
  ]
)

@Library("Infrastructure")

def type = "java"

def product = "private-beta-invitation"

def app = "service"

withPipeline(type, product, app) {
}
