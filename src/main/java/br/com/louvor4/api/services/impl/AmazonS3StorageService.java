package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.exceptions.StorageException;
import br.com.louvor4.api.services.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class AmazonS3StorageService implements StorageService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3Client;

    private static final Logger logger = LoggerFactory.getLogger(AmazonS3StorageService.class);


    public AmazonS3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }


    /**
     * Faz upload de um arquivo para um bucket S3 em uma pasta especificada.
     * Gera um nome único para o arquivo baseado em UUID para evitar colisões.
     * Loga o ETag retornado pelo S3 para verificação do upload.
     *
     * @param file   O arquivo a ser enviado para o S3 (MultipartFile).
     * @param fileCategory A pasta dentro do bucket onde o arquivo será armazenado.
     * @return A URL pública do arquivo armazenado no S3.
     * @throws StorageException Caso ocorra algum erro durante o upload para o S3.
     */
    @Override
    public String uploadFile(MultipartFile file, FileCategory fileCategory) {
        try {
            String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
            String s3Key = buildS3Key(fileCategory, sanitizedFilename);
            return uploadToS3(file, s3Key);
        } catch (IOException e) {
            throw new StorageException("Failed to upload file to S3", e);
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = Optional.ofNullable(originalFilename).orElse("unknown_file");
        return filename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }

    private String buildS3Key(FileCategory fileCategory, String sanitizedFilename) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("%s/%s/%s_%s", fileCategory, datePath, UUID.randomUUID(), sanitizedFilename);
    }

    private String uploadToS3(MultipartFile file, String s3Key) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentLength(file.getSize())
                .contentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectResponse response = s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
            String fileUrl = s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(s3Key)).toExternalForm();
            logger.info("File uploaded successfully: ETag={}, s3Key={}, fileUrl={}", response.eTag(), s3Key, fileUrl);
            return fileUrl;
        }
    }

}
