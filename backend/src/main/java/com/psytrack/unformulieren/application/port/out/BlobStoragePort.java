package com.psytrack.unformulieren.application.port.out;

/**
 * Output port for binary/blob storage operations.
 */
public interface BlobStoragePort {

    /**
     * Uploads content to blob storage and returns the public URL.
     *
     * @param blobName    target filename/path within the container
     * @param content     raw bytes to store
     * @param contentType MIME type (e.g. {@code image/jpeg})
     * @return the URL at which the blob can be accessed
     */
    String upload(String blobName, byte[] content, String contentType);
}
