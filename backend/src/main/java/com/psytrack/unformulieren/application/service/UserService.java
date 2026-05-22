package com.psytrack.unformulieren.application.service;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.psytrack.unformulieren.application.port.out.BlobStoragePort;
import com.psytrack.unformulieren.application.port.out.UserRepositoryPort;
import com.psytrack.unformulieren.domain.model.AppUser;

@Service
public class UserService {

    private static final long MAX_BYTES = 5 * 1024 * 1024L; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepositoryPort userRepositoryPort;
    private final BlobStoragePort blobStoragePort;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepositoryPort userRepositoryPort, BlobStoragePort blobStoragePort,
                       PasswordEncoder passwordEncoder) {
        this.userRepositoryPort = userRepositoryPort;
        this.blobStoragePort = blobStoragePort;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Uploads a new profile picture for the given user and persists the resulting URL.
     *
     * @param username    the authenticated user's username
     * @param content     raw image bytes
     * @param contentType MIME type of the image
     * @return public URL of the uploaded picture
     */
    public String uploadProfilePicture(String username, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (content.length > MAX_BYTES) {
            throw new IllegalArgumentException("File exceeds the 5 MB limit");
        }
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: image/jpeg, image/png, image/webp");
        }

        AppUser user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String blobName = "users/" + user.getId() + "/profile-picture" + extensionFor(contentType);
        String url = blobStoragePort.upload(blobName, content, contentType);

        user.updateProfilePicture(url);
        userRepositoryPort.save(user);

        return url;
    }

    /**
     * Removes the profile picture URL for the given user (does not delete the blob).
     */
    public void removeProfilePicture(String username) {
        AppUser user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.updateProfilePicture(null);
        userRepositoryPort.save(user);
    }

    /**
     * Updates the name, company and job title for the given user.
     */
    public AppUser updateProfileDetails(String username, String name, String company, String jobTitle) {
        AppUser user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.updateProfile(name, user.getPhoneNumber(), company, jobTitle);
        return userRepositoryPort.save(user);
    }

    /**
     * Changes the password for the given user after verifying the current one.
     */
    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
        userRepositoryPort.save(user);
    }

    /**
     * Updates the fully-anonymized privacy flag for the given user.
     */
    public boolean updatePrivacyPreferences(String username, boolean fullyAnonymized) {
        AppUser user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setFullyAnonymized(fullyAnonymized);
        AppUser saved = userRepositoryPort.save(user);
        return saved.isFullyAnonymized();
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> "";
        };
    }
}
