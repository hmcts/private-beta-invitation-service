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
output "test-topic-name" {
  value = "${local.topic_name}"
}

output "test-subscription-name" {
  value = "${local.subscription_name}"
}
#endregion


