variable "product" {
  type    = "string"
}

variable "component" {
  type = "string"
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

variable "capacity" {
  default = "1"
}

variable "subscription" {}
