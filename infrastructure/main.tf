provider "azurerm" {}

locals {
  servicebus_subscription_name = "${var.product}-servicebus-sub-${var.env}"
}

# Make sure the resource group exists
resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"
}

module "servicebus-namespace" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-namespace.git"
  name                  = "${var.product}-servicebus-${var.env}"
  location              = "${var.location}"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
}

module "servicebus-topic" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-topic.git"
  name                  = "${var.product}-servicebus-topic-${var.env}"
  namespace_name        = "${module.servicebus-namespace.name}"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
}

module "servicebus-subscription" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-subscription.git"
  name                  = "${local.servicebus_subscription_name}"
  namespace_name        = "${module.servicebus-namespace.name}"
  topic_name            = "${module.servicebus-topic.name}"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
}

module "service" {
  source   = "git@github.com:contino/moj-module-webapp"
  product  = "${var.product}-${var.component}"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"
  capacity = "${var.capacity}"
  subscription  = "${var.subscription}"
  resource_group_name = "${azurerm_resource_group.rg.name}"

  app_settings = {
    SERVICE_BUS_POLLING_DELAY_MS = "${var.service_bus_polling_delay_ms}"
    SPRING_PROFILES_ACTIVE = "${var.env}"
    # todo: refactor subscription module so that it exposes the conn string in its output.
    SERVICE_BUS_CONNECTION_STRING = "${module.servicebus-topic.primary_send_and_listen_connection_string}/subscriptions/${local.servicebus_subscription_name}"
  }
}
