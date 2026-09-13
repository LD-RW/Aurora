package com.ecommerce.aurora.service;

import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    /**
     * The only extensions an upload may carry, each mapped to the content type the download
     * endpoint will serve it back as. Uploads used to accept any file and keep its original
     * extension, and the download endpoint probed the stored bytes for a content type -- so an
     * uploaded .html file was served back as text/html and executed in this application's
     * origin. Restricting both ends to this table closes that path.
     */
    private static final Map<String, MediaType> ALLOWED_IMAGE_TYPES = Map.of(
            ".png", MediaType.IMAGE_PNG,
            ".jpg", MediaType.IMAGE_JPEG,
            ".jpeg", MediaType.IMAGE_JPEG,
            ".gif", MediaType.IMAGE_GIF,
            ".webp", MediaType.parseMediaType("image/webp")
    );

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new APIException("Image file is required");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_TYPES.containsKey(extension)) {
            throw new APIException("Unsupported image type. Allowed types: png, jpg, jpeg, gif, webp");
        }

        String declaredContentType = file.getContentType();
        if (declaredContentType == null || !declaredContentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new APIException("Unsupported image type. Allowed types: png, jpg, jpeg, gif, webp");
        }

        // The extension and the client-declared content type are both attacker-controlled, so
        // neither proves the bytes are an image. Decoding the stream is what actually does:
        // ImageIO returns null for anything it can't parse as one.
        assertDecodesAsImage(file);

        String fileName = UUID.randomUUID() + extension;
        String filePath = path + File.separator + fileName;

        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        Files.copy(file.getInputStream(), Paths.get(filePath));
        return fileName;
    }

    @Override
    public Resource loadImageAsResource(String path, String fileName) throws MalformedURLException {
        Path baseDirectory = Paths.get(path).toAbsolutePath().normalize();
        Path requestedFile = baseDirectory.resolve(fileName).normalize();

        // fileName is attacker-controlled input from a URL path segment. resolve()
        // does not on its own stop "../../etc/passwd" or an absolute path from
        // escaping baseDirectory -- normalize() collapses ".." segments, and this
        // check confirms the result still lives inside baseDirectory before the
        // filesystem is ever touched.
        if (!requestedFile.startsWith(baseDirectory)) {
            throw new ResourceNotFoundException("Image", "fileName", fileName);
        }

        Resource resource = new UrlResource(requestedFile.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Image", "fileName", fileName);
        }

        return resource;
    }

    /**
     * Maps a stored file name to the content type it may be served as, defaulting to an opaque
     * download for anything outside the allowlist (including files stored before uploads were
     * restricted).
     */
    public static MediaType mediaTypeForFileName(String fileName) {
        return ALLOWED_IMAGE_TYPES.getOrDefault(extensionOf(fileName), MediaType.APPLICATION_OCTET_STREAM);
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private void assertDecodesAsImage(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage decoded = ImageIO.read(inputStream);
            if (decoded == null) {
                throw new APIException("Uploaded file is not a readable image");
            }
        }
    }
}
