package org.elsveys.service;

import org.elsveys.model.FileMetadata;
import org.elsveys.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileMetadataRepository fileRepository;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    @Test
    void testUploadFileSuccess() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("test.kt");
        when(mockFile.getBytes()).thenReturn("test content".getBytes());
        when(mockFile.getSize()).thenReturn(12L);
        when(fileRepository.findByUploaderId(1L)).thenReturn(List.of());

        FileMetadata savedMetadata = new FileMetadata();
        savedMetadata.setFileId(1L);
        savedMetadata.setName("test.kt");
        savedMetadata.setType("kt");

        when(fileRepository.save(any(FileMetadata.class))).thenReturn(savedMetadata);

        FileMetadata result = fileService.uploadFile(mockFile, 1L, "testUser");

        assertNotNull(result);
        assertEquals("test.kt", result.getName());
        assertEquals("kt", result.getType());
        verify(fileRepository, times(1)).save(any(FileMetadata.class));
    }

    @Test
    void testUploadAnyFileType() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockFile.getBytes()).thenReturn("test content".getBytes());
        when(mockFile.getSize()).thenReturn(12L);
        when(fileRepository.findByUploaderId(1L)).thenReturn(List.of());

        FileMetadata savedMetadata = new FileMetadata();
        savedMetadata.setFileId(1L);
        savedMetadata.setName("test.txt");
        savedMetadata.setType("txt");

        when(fileRepository.save(any(FileMetadata.class))).thenReturn(savedMetadata);

        FileMetadata result = fileService.uploadFile(mockFile, 1L, "testUser");

        assertNotNull(result);
        assertEquals("test.txt", result.getName());
        assertEquals("txt", result.getType());
        verify(fileRepository, times(1)).save(any(FileMetadata.class));
    }

    @Test
    void testUploadFileDuplicate() {
        when(mockFile.getOriginalFilename()).thenReturn("duplicate.kt");

        FileMetadata existing = new FileMetadata();
        existing.setName("duplicate.kt");
        when(fileRepository.findByUploaderId(1L)).thenReturn(List.of(existing));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileService.uploadFile(mockFile, 1L, "testUser");
        });

        assertEquals("File with this name already exists", exception.getMessage());
        verify(fileRepository, never()).save(any(FileMetadata.class));
    }

    @Test
    void testUploadJpgFile() throws IOException {
        when(mockFile.getOriginalFilename()).thenReturn("image.jpg");
        when(mockFile.getBytes()).thenReturn(new byte[100]);
        when(mockFile.getSize()).thenReturn(100L);
        when(fileRepository.findByUploaderId(1L)).thenReturn(List.of());

        FileMetadata savedMetadata = new FileMetadata();
        savedMetadata.setFileId(2L);
        savedMetadata.setName("image.jpg");
        savedMetadata.setType("jpg");

        when(fileRepository.save(any(FileMetadata.class))).thenReturn(savedMetadata);

        FileMetadata result = fileService.uploadFile(mockFile, 1L, "testUser");

        assertNotNull(result);
        assertEquals("image.jpg", result.getName());
        assertEquals("jpg", result.getType());
    }

    @Test
    void testDeleteFile() throws IOException {
        Path testFile = tempDir.resolve("test.kt");
        Files.write(testFile, "test content".getBytes());

        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(1L);
        metadata.setFilePath(testFile.toString());

        when(fileRepository.findById(1L)).thenReturn(Optional.of(metadata));
        doNothing().when(fileRepository).deleteById(1L);

        assertDoesNotThrow(() -> fileService.deleteFile(1L));
        verify(fileRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteFileNotFound() {
        when(fileRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileService.deleteFile(1L);
        });

        assertEquals("File not found", exception.getMessage());
    }

    @Test
    void testGetFileMetadata() {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(1L);
        metadata.setName("test.kt");

        when(fileRepository.findById(1L)).thenReturn(Optional.of(metadata));

        FileMetadata result = fileService.getFileMetadata(1L);

        assertNotNull(result);
        assertEquals("test.kt", result.getName());
    }

    @Test
    void testListAllFiles() {
        FileMetadata file1 = new FileMetadata();
        file1.setName("file1.kt");
        FileMetadata file2 = new FileMetadata();
        file2.setName("file2.jpg");

        when(fileRepository.findAll()).thenReturn(Arrays.asList(file1, file2));

        List<FileMetadata> result = fileService.listAllFiles();

        assertEquals(2, result.size());
        verify(fileRepository, times(1)).findAll();
    }

    @Test
    void testUpdateFileSuccess() throws IOException {
        Path existingFile = tempDir.resolve("1_old.kt");
        Files.write(existingFile, "old content".getBytes());

        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileId(1L);
        existingMetadata.setName("old.kt");
        existingMetadata.setType("kt");
        existingMetadata.setFilePath(existingFile.toString());
        existingMetadata.setUploaderId(1L);
        existingMetadata.setUploaderName("uploader");
        existingMetadata.setEditorId(1L);
        existingMetadata.setEditorName("uploader");

        when(fileRepository.findById(1L)).thenReturn(Optional.of(existingMetadata));

        when(mockFile.getOriginalFilename()).thenReturn("new.kt");
        when(mockFile.getBytes()).thenReturn("new content".getBytes());
        when(mockFile.getSize()).thenReturn(11L);

        FileMetadata updatedMetadata = new FileMetadata();
        updatedMetadata.setFileId(1L);
        updatedMetadata.setName("new.kt");
        updatedMetadata.setType("kt");
        updatedMetadata.setEditorId(2L);
        updatedMetadata.setEditorName("editor");

        when(fileRepository.save(any(FileMetadata.class))).thenReturn(updatedMetadata);

        FileMetadata result = fileService.updateFile(1L, mockFile, 2L, "editor");

        assertNotNull(result);
        assertEquals("new.kt", result.getName());
        assertEquals(2L, result.getEditorId());
        assertEquals("editor", result.getEditorName());
        assertFalse(Files.exists(existingFile));
        verify(fileRepository, times(1)).save(any(FileMetadata.class));
    }

    @Test
    void testUpdateFileNotFound() {
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileService.updateFile(999L, mockFile, 1L, "editor");
        });

        assertEquals("File not found", exception.getMessage());
        verify(fileRepository, never()).save(any(FileMetadata.class));
    }

    @Test
    void testUpdateFileChangesType() throws IOException {
        Path existingFile = tempDir.resolve("1_document.kt");
        Files.write(existingFile, "text content".getBytes());

        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileId(1L);
        existingMetadata.setName("document.kt");
        existingMetadata.setType("kt");
        existingMetadata.setFilePath(existingFile.toString());
        existingMetadata.setUploaderId(1L);

        when(fileRepository.findById(1L)).thenReturn(Optional.of(existingMetadata));
        when(mockFile.getOriginalFilename()).thenReturn("image.jpg");
        when(mockFile.getBytes()).thenReturn(new byte[100]);
        when(mockFile.getSize()).thenReturn(100L);

        FileMetadata updatedMetadata = new FileMetadata();
        updatedMetadata.setFileId(1L);
        updatedMetadata.setName("image.jpg");
        updatedMetadata.setType("jpg");

        when(fileRepository.save(any(FileMetadata.class))).thenReturn(updatedMetadata);

        FileMetadata result = fileService.updateFile(1L, mockFile, 1L, "user");

        assertEquals("image.jpg", result.getName());
        assertEquals("jpg", result.getType());
    }
}