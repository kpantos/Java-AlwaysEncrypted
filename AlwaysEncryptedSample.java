package com.example;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.sqlserver.jdbc.*;
import java.sql.*;
import java.util.Properties;

public class AlwaysEncryptedSample {
    public static void main(String[] args) throws Exception {
        // JDBC connection string
        // Sample connection string jdbc:sqlserver://<your-sql-server>.database.windows.net:1433;database=<your-database>;encrypt=true;authentication=ActiveDirectoryManagedIdentity;
        // picked from akv using the csi driver
        String connectionUrl = System.getenv("SQL_CONN_STRING");
        if (connectionUrl == null) {
            System.err.println("SQL_CONN_STRING environment variable not set.");
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
        props.setProperty("keyStoreAuthentication", "AzureKeyVaultManagedIdentity");
        props.setProperty("keyStorePrincipalId", System.getenv("AZURE_MANAGED_IDENTITY_CLIENT_ID")); 
        // Add more properties as needed

        try (Connection conn = DriverManager.getConnection(connectionUrl, props)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM [YourTable] WHERE [EncryptedColumn] = ?");
            while (rs.next()) {
                System.out.println("Row: " + rs.getString(1));
            }
        }
    }
}
