variable "product" {
  type    = "string"
}

variable "raw_product" {
  default = "pbi" // jenkins-library overrides `product` for PRs and adds e.g. pr-118-pbi
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

variable "tenant_id" {}

variable "jenkins_AAD_objectId" {
  type        = "string"
  description = "(Required) The Azure AD object ID of a user, service principal or security group in the Azure Active Directory tenant for the vault. The object ID must be unique for the list of access policies."
}

variable "capacity" {
  default = "1"
}

variable "subscription" {}

variable "common_tags" {
  type = "map"
}

variable "team_contact" {
  default     = "#rpe"
  description = "Slack channel team can be reached on for support"
}
