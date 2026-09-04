package com.ecommerce.aurora.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;


public interface FileService {
    String uploadImage(String path, MultipartFile file) throws IOException;

    Resource loadImageAsResource(String path, String fileName) throws MalformedURLException;
}
