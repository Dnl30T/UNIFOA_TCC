package com.psytrack.unformulieren.adapter.out.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.psytrack.unformulieren.application.port.out.BlobStoragePort;
import org.springframework.stereotype.Component;

@Component
public class BlobStorageAdapter implements BlobStoragePort {

    private final BlobContainerClient containerClient;

    public BlobStorageAdapter(BlobContainerClient containerClient) {
        this.containerClient = containerClient;
    }

    @Override
    public String upload(String blobName, byte[] content, String contentType) {
        if (!containerClient.exists()) {
            containerClient.create();
        }
        var blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(BinaryData.fromBytes(content), true);
        blobClient.setHttpHeaders(
                new com.azure.storage.blob.models.BlobHttpHeaders().setContentType(contentType));
        return blobClient.getBlobUrl();
    }
}
