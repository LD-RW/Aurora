package com.ecommerce.aurora.service;

import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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

    // Uploads used to accept any file and keep its extension, and the download endpoint derived
    // a content type by probing the stored bytes -- so an uploaded HTML file came back as
    // text/html and any script in it ran in the application's own origin.

    @Test
    void rejectsAnHtmlUploadThatWouldOtherwiseBeServedBackAsExecutableMarkup(@TempDir Path tempDir) {
        MockMultipartFile htmlDisguisedAsUpload = new MockMultipartFile(
                "Image", "payload.html", MediaType.TEXT_HTML_VALUE,
                "<html><body><script>alert(1)</script></body></html>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> fileService.uploadImage(tempDir.toString(), htmlDisguisedAsUpload))
                .isInstanceOf(APIException.class);

        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void rejectsANonImageEvenWhenItClaimsAnImageExtensionAndContentType(@TempDir Path tempDir) {
        // Both the extension and the declared content type come from the client, so neither
        // proves anything on its own -- only decoding the bytes does.
        MockMultipartFile scriptWearingAPngName = new MockMultipartFile(
                "Image", "not-really.png", MediaType.IMAGE_PNG_VALUE,
                "#!/bin/bash\nrm -rf /".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> fileService.uploadImage(tempDir.toString(), scriptWearingAPngName))
                .isInstanceOf(APIException.class);

        assertThat(tempDir).isEmptyDirectory();
    }

    @Test
    void acceptsARealImageAndStoresItUnderAGeneratedName(@TempDir Path tempDir) throws IOException {
        MockMultipartFile realPng = new MockMultipartFile(
                "Image", "photo.png", MediaType.IMAGE_PNG_VALUE, onePixelPng());

        String storedFileName = fileService.uploadImage(tempDir.toString(), realPng);

        assertThat(storedFileName).endsWith(".png").doesNotContain("photo");
        assertThat(tempDir.resolve(storedFileName)).exists();
    }

    @Test
    void servesKnownImageExtensionsAsImagesAndAnythingElseAsAnOpaqueDownload() {
        assertThat(FileServiceImpl.mediaTypeForFileName("a.png")).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(FileServiceImpl.mediaTypeForFileName("a.JPG")).isEqualTo(MediaType.IMAGE_JPEG);
        // Covers anything stored before uploads were restricted.
        assertThat(FileServiceImpl.mediaTypeForFileName("legacy.html"))
                .isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(FileServiceImpl.mediaTypeForFileName("no-extension"))
                .isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }

    private static byte[] onePixelPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }
}
