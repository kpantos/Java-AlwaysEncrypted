package com.example;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.microsoft.sqlserver.jdbc.*;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

public class AlwaysEncryptedSample {
    public static void main(String[] args) throws Exception {
        
        // System.out.println("Test Azure key vault access");
        // testKeyVaultAccess();
        
        System.out.println("Querying database with Always Encrypted");
        queryDatabase();

        System.out.println("Waiting indefinitely. Press Ctrl+C to exit.");
        Thread.currentThread().join();
    }

    private static String getSqlConnectionStringFromKeyVault() {
        String keyVaultUrl = System.getenv("KEYVAULT_URL");
        String secretName = System.getenv("SECRET_NAME");
        if (keyVaultUrl == null || secretName == null) {
            throw new RuntimeException("KEYVAULT_URL or SECRET_NAME environment variable not set.");
        }
        SecretClient client = new SecretClientBuilder()
                .vaultUrl(keyVaultUrl)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        KeyVaultSecret secret = client.getSecret(secretName);
        return secret.getValue();
    }

    private static void queryDatabase() throws Exception {
        String connectionUrl = getSqlConnectionStringFromKeyVault();
        if (connectionUrl == null) {
            System.err.println("SQL connection string not found in Key Vault.");
            System.exit(1);
        }

        // Register the Azure Key Vault provider for Always Encrypted
        SQLServerColumnEncryptionAzureKeyVaultProvider akvProvider =
            new SQLServerColumnEncryptionAzureKeyVaultProvider(
                new DefaultAzureCredentialBuilder().build()
            );
        SQLServerConnection.registerColumnEncryptionKeyStoreProviders(
            java.util.Collections.singletonMap("AZURE_KEY_VAULT", akvProvider)
        );

        // Set connection properties
        //columnEncryptionSetting=Enabled;keyStoreAuthentication=AzureKeyVaultManagedIdentity;keyStorePrincipalId=<your-managed-identity-client-id>
        Properties props = new Properties();
        props.setProperty("columnEncryptionSetting", "Enabled");
        props.setProperty("keyStoreAuthentication", "KeyVaultManagedIdentity");
        props.setProperty("keyStorePrincipalId", System.getenv("AZURE_MANAGED_IDENTITY_CLIENT_ID")); 
        // Add more properties as needed

        try (Connection conn = DriverManager.getConnection(connectionUrl, props)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM [Sales].Employees;");
            while (rs.next()) {
                System.out.println("ID: " + rs.getString(1) + " SSN: " + rs.getString(2));
            }
        }
    }

    private static void testKeyVaultAccess() throws Exception {

        Map<String, String> env = System.getenv();
        String keyVaultUrl = env.get("KEYVAULT_URL");
        String secretName = env.get("SECRET_NAME");

        if (keyVaultUrl != null && secretName != null) {
            System.out.println("Key Vault URL: " + keyVaultUrl);
            System.out.println("Secret Name: " + secretName);

            SecretClient client = new SecretClientBuilder()
                    .vaultUrl(keyVaultUrl)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
            KeyVaultSecret secret = client.getSecret(secretName);

            System.out.println("Secret Value: " + secret.getValue());
        }
    }
}
