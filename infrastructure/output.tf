output "vaultUri" {
  value = "${data.azurerm_key_vault.vault.vault_uri}"
}

output "vaultName" {
  value = "${local.vault_name}"
}

output "microserviceName" {
  value = "${var.component}"
}

#region Env vars for smoke test (transformed by Jenkins)
output "TEST_SERVICE_BUS_TOPIC" {
  value = "${local.topic_name}"
}

output "TEST_SERVICE_BUS_SUBSCRIPTION" {
  value = "${local.subscription_name}"
}
#endregion


