provider "azurerm" {}

locals {
  subscription_name = "pbi"
  topic_name = "invitations"

  preview_vault_name     = "${var.raw_product}-aat"
  default_vault_name     = "${var.raw_product}-${var.env}"
  vault_name             = "${(var.env == "preview" || var.env == "spreview") ? local.preview_vault_name : local.default_vault_name}"

  preview_vault_rg       = "${var.raw_product}-aat"
  default_vault_rg       = "${var.raw_product}-${var.env}"
  vault_rg               = "${(var.env == "preview" || var.env == "spreview") ? local.preview_vault_rg : local.default_vault_rg}"
  sku_size = "${var.env == "prod" || var.env == "sprod" || var.env == "aat" ? "I2" : "I1"}"

  tags = "${merge(
    var.common_tags,
    map(
      "Team Contact", var.team_contact
    )
  )}"
}

# Make sure the resource group exists
resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"

  tags = "${local.tags}"
}

resource "azurerm_resource_group" "vault_rg" {
  name     = "${local.vault_rg}"
  location = "${var.location}"
}

module "servicebus-namespace" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-namespace.git"
  name                  = "${var.product}-servicebus-${var.env}"
  location              = "${var.location}"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
  env                   = "${var.env}"
  common_tags           = "${var.common_tags}"
}

module "servicebus-topic" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-topic.git"
  name                  = "${local.topic_name}"
  namespace_name        = "${module.servicebus-namespace.name}"
  resource_group_name   = "${azurerm_resource_group.rg.name}"
}

module "servicebus-subscription" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-subscription.git"
  name                  = "${local.subscription_name}"
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
  common_tags         = "${var.common_tags}"
  asp_name      = "${var.product}-${var.component}-${var.env}"
  asp_rg        = "${var.product}-${var.component}-${var.env}"
  instance_size = "${local.sku_size}"

  app_settings = {
    SERVICE_BUS_POLLING_DELAY_MS = "${var.service_bus_polling_delay_ms}"
    # todo: refactor subscription module so that it exposes the conn string in its output.
    SERVICE_BUS_CONNECTION_STRING = "${module.servicebus-topic.primary_send_and_listen_connection_string}/subscriptions/${local.subscription_name}"

    TEST_SERVICE_NOTIFY_API_KEY = "${data.azurerm_key_vault_secret.test_service_notify_api_key.value}"
  }
}

module "key-vault" {
  source              = "git@github.com:hmcts/moj-module-key-vault?ref=master"
  name                = "${local.vault_name}"
  product             = "${var.product}"
  env                 = "${var.env}"
  tenant_id           = "${var.tenant_id}"
  object_id           = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${azurerm_resource_group.vault_rg.name}"
  # dcd_cc-dev group object ID
  product_group_object_id = "38f9dea6-e861-4a50-9e73-21e64f563537"
}

data "azurerm_key_vault_secret" "test_service_notify_api_key" {
  name = "test-service-notify-api-key"
  vault_uri = "${module.key-vault.key_vault_uri}"
}

# Store connection string in vault for other services / tests to use
resource "azurerm_key_vault_secret" "servicebus_conn_string" {
  name = "servicebus-conn-string"
  vault_uri = "${module.key-vault.key_vault_uri}"
  value = "${module.servicebus-topic.primary_send_and_listen_connection_string}"
}
