package br.com.louvor4.api.services;

import br.com.louvor4.api.enums.FileCategory;
import org.springframework.web.multipart.MultipartFile;


public interface StorageService {
    String uploadFile(MultipartFile file, FileCategory folder);
}
