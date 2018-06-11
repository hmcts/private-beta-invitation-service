variable "product" {
  type    = "string"
  default = "private-beta-invitation"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "service_bus_polling_delay_ms" {
    type = "string"
    default = "30000"
    description = "Delay between Azure Service Bus subscription polling, in milliseconds"
}

variable "ilbIp"{}
variable "subscription" {}
