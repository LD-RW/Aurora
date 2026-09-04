package com.ecommerce.aurora.service;

import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {

        String originalFilename = file.getOriginalFilename();
        String randomId = UUID.randomUUID().toString();

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf(".");

        if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex);
        }

        String fileName = randomId.concat(extension);
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
}
