package com.ecommerce.aurora.service;

import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileServiceImplTest {

    private final FileServiceImpl fileService = new FileServiceImpl();

    @Test
    void loadsAnExistingFileAsAReadableResource(@TempDir Path tempDir) throws IOException {
        Path imageFile = tempDir.resolve("product-42.png");
        Files.writeString(imageFile, "fake-image-bytes", StandardCharsets.UTF_8);

        Resource resource = fileService.loadImageAsResource(tempDir.toString(), "product-42.png");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.getFile()).hasContent("fake-image-bytes");
    }

    @Test
    void throwsNotFoundForAFileThatDoesNotExist(@TempDir Path tempDir) {
        assertThatThrownBy(() -> fileService.loadImageAsResource(tempDir.toString(), "does-not-exist.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsNotFoundForAPathTraversalAttemptEvenWhenTheTargetFileReallyExists(@TempDir Path tempDir) throws IOException {
        // The base directory passed to loadImageAsResource is the "images" subdirectory,
        // not tempDir itself -- secret.txt lives one level above it. If "../secret.txt"
        // successfully escaped the base directory, it would resolve to a file that
        // genuinely exists, so this proves the containment check itself is what blocks
        // it, not a coincidental "file not found".
        Path imagesDirectory = tempDir.resolve("images");
        Files.createDirectory(imagesDirectory);
        Path secretFile = tempDir.resolve("secret.txt");
        Files.writeString(secretFile, "top secret", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> fileService.loadImageAsResource(imagesDirectory.toString(), "../secret.txt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
