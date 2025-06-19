# Use the official .NET 8 SDK image for build
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

# Copy csproj and restore as distinct layers
COPY AkvAlwaysEncryptedSample.csproj ./
RUN dotnet restore AkvAlwaysEncryptedSample.csproj

# Copy the rest of the source code
COPY . .

# Build the application
RUN dotnet publish AkvAlwaysEncryptedSample.csproj -c Release -o /app/publish --no-restore

# Use the official .NET 8 runtime image for the app
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS runtime
WORKDIR /app
COPY --from=build /app/publish .

# Set environment variables for Azure Workload Identity (optional, for local dev)
# ENV AZURE_CLIENT_ID=<client-id>
# ENV AZURE_TENANT_ID=<tenant-id>
# ENV AZURE_FEDERATED_TOKEN_FILE=<token-file-path>

ENTRYPOINT ["dotnet", "AkvAlwaysEncryptedSample.dll"]
