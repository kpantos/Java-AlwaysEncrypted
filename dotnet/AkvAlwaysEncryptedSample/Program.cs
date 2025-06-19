// .NET 8 minimal sample for Always Encrypted with Azure Key Vault and user-assigned managed identity
// Place this in dotnet/AkvAlwaysEncryptedSample/Program.cs
using System;
using System.Collections.Generic;
using System.Security.Cryptography;
using Azure.Identity;
using Azure.Core;
using Microsoft.Data.SqlClient;
using Microsoft.Data.SqlClient.AlwaysEncrypted.AzureKeyVaultProvider;

namespace AkvAlwaysEncryptedSample
{
    public class Program
    {
        // Read connection string from environment variable
        private static readonly string s_connectionString = Environment.GetEnvironmentVariable("SQL_CONNECTION_STRING");
        //private static readonly string s_userAssignedClientId = "{UserAssignedManagedIdentityClientId}";


        public static void Main(string[] args)
        {
            // Use DefaultAzureCredential with user-assigned managed identity
            // var credential = new DefaultAzureCredential(new DefaultAzureCredentialOptions
            // {
            //     ManagedIdentityClientId = s_userAssignedClientId
            // });
            var credential = new DefaultAzureCredential();

            var akvProvider = new SqlColumnEncryptionAzureKeyVaultProvider(credential);
            SqlConnection.RegisterColumnEncryptionKeyStoreProviders(new Dictionary<string, SqlColumnEncryptionKeyStoreProvider>(1, StringComparer.OrdinalIgnoreCase)
            {
                { SqlColumnEncryptionAzureKeyVaultProvider.ProviderName, akvProvider }
            });
            Console.WriteLine("AKV provider Registered");

            using (SqlConnection sqlConnection = new SqlConnection(s_connectionString))
            {
                try
                {
                    sqlConnection.Open();
                    using (SqlCommand command = sqlConnection.CreateCommand())
                    {
                        command.CommandText = "SELECT * FROM [Sales].Employees;";
                        using (SqlDataReader sqlDataReader = command.ExecuteReader())
                        {
                            while (sqlDataReader.Read())
                            {
                                // Assuming the first column is an integer ID, the second is a string ID, and the third is a string SSN
                                Console.WriteLine("ID: " + sqlDataReader.GetInt32(0) + " SSN: " + sqlDataReader.GetString(1));
                            }
                        }
                    }
                }
                finally
                {
                    if (sqlConnection.State == System.Data.ConnectionState.Open)
                    {
                        sqlConnection.Close();
                    }
                }
                Console.WriteLine("Completed AKV provider Sample.");
            }

            System.Threading.Thread.Sleep(System.Threading.Timeout.Infinite);
        }
    }
}
