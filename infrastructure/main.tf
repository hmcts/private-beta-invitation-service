provider "azurerm" {}

module "service" {
  source   = "git@github.com:contino/moj-module-webapp"
  product  = "${var.product}-service"
  location = "${var.location}"
  env      = "${var.env}"
  ilbIp    = "${var.ilbIp}"

  app_settings = {
    SERVICE_BUS_POLLING_DELAY_MS = "${var.service_bus_polling_delay_ms}"
    SPRING_PROFILES_ACTIVE = "${var.env}"
  }
}
