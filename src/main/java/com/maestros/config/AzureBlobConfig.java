package com.maestros.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobConfig {

    @Value("${AZURE_STORAGE_CONNECTION_STRING:}")
    private String connectionString;

    @Value("${AZURE_STORAGE_CONTAINER_NAME:maestros-media}")
    private String containerName;

    @Bean
    @ConditionalOnProperty("AZURE_STORAGE_CONNECTION_STRING")
    public BlobContainerClient blobContainerClient() {
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        return serviceClient.getBlobContainerClient(containerName);
    }
}
