package org.elsveys.service;

import org.elsveys.model.FileMetadata;
import org.elsveys.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class FileService {

    @Autowired
    private FileMetadataRepository fileRepository;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    public FileMetadata uploadFile(MultipartFile file, Long uploaderId, String uploaderName) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);

        List<FileMetadata> existingFiles = fileRepository.findByUploaderId(uploaderId);
        for (FileMetadata existing : existingFiles) {
            if (existing.getName().equals(originalName)) {
                throw new RuntimeException("File with this name already exists");
            }
        }

        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            uploadDirectory.mkdirs();
        }

        String storedName = uploaderId + "_" + originalName;
        Path filePath = Paths.get(uploadDir, storedName);
        Files.write(filePath, file.getBytes());

        FileMetadata metadata = new FileMetadata();
        metadata.setName(originalName);
        metadata.setType(extension);
        metadata.setSize(file.getSize());
        metadata.setFilePath(filePath.toString());
        metadata.setUploaderId(uploaderId);
        metadata.setUploaderName(uploaderName);
        metadata.setEditorId(uploaderId);
        metadata.setEditorName(uploaderName);

        return fileRepository.save(metadata);
    }

    public byte[] downloadFile(Long fileId) throws IOException {
        FileMetadata metadata = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Path path = Paths.get(metadata.getFilePath());
        return Files.readAllBytes(path);
    }

    public void deleteFile(Long fileId) throws IOException {
        FileMetadata metadata = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Path path = Paths.get(metadata.getFilePath());
        Files.deleteIfExists(path);
        fileRepository.deleteById(fileId);
    }

    public List<FileMetadata> listAllFiles() {
        return fileRepository.findAll();
    }

    public FileMetadata getFileMetadata(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex + 1).toLowerCase() : "";
    }

    public FileMetadata updateFile(Long fileId, MultipartFile file, Long editorId, String editorName) throws IOException {
        FileMetadata metadata = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);

        Path oldPath = Paths.get(metadata.getFilePath());
        Files.deleteIfExists(oldPath);

        String storedName = metadata.getUploaderId() + "_" + originalName;
        Path newFilePath = Paths.get(uploadDir, storedName);
        Files.write(newFilePath, file.getBytes());

        metadata.setName(originalName);
        metadata.setType(extension);
        metadata.setSize(file.getSize());
        metadata.setFilePath(newFilePath.toString());
        metadata.setEditorId(editorId);
        metadata.setEditorName(editorName);
        return fileRepository.save(metadata);
    }
}