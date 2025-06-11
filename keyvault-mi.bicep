// Bicep file to create a managed identity and grant it access to Azure Key Vault for column encryption secrets

param location string = resourceGroup().location
param managedIdentityName string = 'aks-keyvault-mi'
param keyVaultName string
param keyVaultResourceId string
param keyVaultAccessObjectId string = '' // Optional: for custom objectId assignment

resource userAssignedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: managedIdentityName
  location: location
}

resource keyVault 'Microsoft.KeyVault/vaults@2023-02-01' existing = {
  name: keyVaultName
}

resource keyVaultAccessPolicy 'Microsoft.KeyVault/vaults/accessPolicies@2023-02-01' = {
  name: '${keyVault.name}/add'
  properties: {
    accessPolicies: [
      {
        tenantId: keyVault.properties.tenantId
        objectId: userAssignedIdentity.properties.principalId
        permissions: {
          secrets: [
            'get'
            'list'
          ]
          keys: [
            'get'
            'list'
            'unwrapKey'
            'wrapKey'
          ]
        }
      }
    ]
  }
}

output managedIdentityClientId string = userAssignedIdentity.properties.clientId
output managedIdentityPrincipalId string = userAssignedIdentity.properties.principalId
