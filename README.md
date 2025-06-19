# Sample Always Encrypted App with Azure Key Vault and AKS Workload Identities

This sample demonstrates a Java application using SQL Server Always Encrypted with Azure Key Vault as the column master key store. It is designed for containerized deployment in AKS, leveraging the Azure Key Vault for secret management.

## Prerequisites
- Azure SQL Database or SQL Server with Always Encrypted enabled
- Column Masredister Key (CMK) stored in Azure Key Vault
- AKS cluster with Workload Identity enabled
- User managed identity with access to Key Vault

## Configuration
- Set the `SQL_CONN_STRING` environment variable with your JDBC connection string (should include `columnEncryptionSetting=Enabled`).
- Set the `AZURE_MANAGED_IDENTITY_CLIENT_ID` for setting the managed identity with access to the column encryption keys at the keyvault
- Set the `AZURE_TENANT_ID` for setting the managed identity with access to the column encryption keys at the keyvault
- Set the `KEYVAULT_URL` to access the secrets.
- Store the connection string to SQL server as a secret to the KeyVault with the name `SQLConnection`

## Build and Run Locally
```sh
mvn package
java -cp target/always-encrypted-sample-1.0-SNAPSHOT.jar;target/lib/* com.example.AlwaysEncryptedSample
```

## Containerization
A sample `Dockerfile` is provided:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS build
COPY . /app
WORKDIR /app
RUN mvn package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/always-encrypted-sample-1.0-SNAPSHOT.jar .
COPY --from=build /app/target/lib ./lib
ENTRYPOINT ["java", "-cp", "always-encrypted-sample-1.0-SNAPSHOT.jar:lib/*", "com.example.AlwaysEncryptedSample"]
```

>! *NOTE* for convinience an image is already built and pushed to DockerHub at https://hub.docker.com/r/kpantos/com.example.alwaysencrypted/tags

To ensure your AKS pod is assigned a managed identity, you should use Azure Workload Identity (the recommended approach as Azure AD Pod Identity is deprecated). Here’s how you can do it:

## Allow access to keyvault with a managed identity
**1. Create a User-Assigned Managed Identity (UAMI):**
Use the `keyvault-mi.bicep` file to deploy a managed identity. 

**2. Grant the UAMI access to Key Vault and SQL Server:**
Assign it the required roles for accessing secrets at the keyvault. This is also handled in your Bicep.

**3. Enable Workload Identity on your AKS cluster:**
If workload identity is not enabled on your cluster use the following command to enable it.
```sh
az aks update --name <aks-name> --resource-group <rg> --enable-oidc-issuer --enable-workload-identity
```

**4. Create a federated identity credential for the UAMI:**
```sh
az identity federated-credential create \
  --name <federated-credential-name> \
  --identity-name <uami-name> \
  --resource-group <rg> \
  --issuer <aks-oidc-issuer-url> \
  --subject system:serviceaccount:<namespace>:<service-account-name>
```
- `<aks-oidc-issuer-url>` can be found with:  
  `az aks show --name <aks-name> --resource-group <rg> --query "oidcIssuerProfile.issuerUrl" -o tsv`

**5. Create a Kubernetes ServiceAccount annotated for workload identity:**
Add this to your manifest:
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: <service-account-name>
  namespace: <namespace>
  annotations:
    azure.workload.identity/client-id: <uami-client-id>
```
This is provided at the `aks-serviceaccount.yaml`file.

**6. Reference the ServiceAccount in your Deployment:**
In your deployment YAML:
```yaml
spec:
  serviceAccountName: <service-account-name>
```

**Summary:**  
- Use a ServiceAccount annotated with the managed identity’s client ID.
- Reference that ServiceAccount in your pod/deployment.
- Ensure the federated identity credential is created for the UAMI.
- Make sure your AKS cluster has workload identity enabled.



## References
- [Always Encrypted with JDBC](https://learn.microsoft.com/en-us/sql/connect/jdbc/using-always-encrypted-with-the-jdbc-driver?view=sql-server-ver17)
- [Deploy and configure workload identity on an Azure Kubernetes Service (AKS) cluster](https://learn.microsoft.com/en-us/azure/aks/workload-identity-deploy-cluster)
- [Workload Identity QuickStart](https://azure.github.io/azure-workload-identity/docs/quick-start.html)
- [Workload Identity vs CSI Secrets Store Driver](workload-identity-csi.md)
