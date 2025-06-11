# Sample Java Always Encrypted App with Azure Key Vault and AKS CSI Driver

This sample demonstrates a Java application using SQL Server Always Encrypted with Azure Key Vault as the column master key store. It is designed for containerized deployment in AKS, leveraging the Azure Key Vault CSI driver for secret management.

## Prerequisites
- Azure SQL Database or SQL Server with Always Encrypted enabled
- Column Master Key (CMK) stored in Azure Key Vault
- AKS cluster with Azure Key Vault CSI driver installed and configured
- Service principal or managed identity with access to Key Vault

## Configuration
- Set the `SQL_CONN_STRING` environment variable with your JDBC connection string (should include `columnEncryptionSetting=Enabled`).
- Set the `AZURE_MANAGED_IDENTITY_CLIENT_ID` for setting the managed identity with access to the column encryption keys
- The application uses `DefaultAzureCredential` for authentication (supports managed identity in AKS).
- The CSI driver should mount any required secrets (if needed) as files or environment variables.

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

## AKS Deployment
- Use a Kubernetes `Deployment` manifest to deploy the container.
- Use the CSI driver to mount Key Vault secrets if needed.
- Ensure the pod has access to Key Vault (via managed identity or service principal).

## References
- [Always Encrypted with JDBC](https://learn.microsoft.com/en-us/sql/connect/jdbc/using-always-encrypted-with-the-jdbc-driver?view=sql-server-ver17)
- [Azure Key Vault CSI Driver](https://learn.microsoft.com/en-us/azure/aks/csi-secrets-store-driver)
