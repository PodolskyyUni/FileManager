package org.elsveys.contoller;

import org.elsveys.model.FileMetadata;
import org.elsveys.service.AuthService;
import org.elsveys.service.FileService;
import org.elsveys.service.SortFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private SortFilterService sortFilterService;

    @Autowired
    private AuthService authService;

    private Long getUserIdFromHeader(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return authService.getUserIdFromToken(token);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = authService.getUserIdFromToken(token);
            String username = authService.getUsernameFromToken(token);

            FileMetadata metadata = fileService.uploadFile(file, userId, username);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long fileId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            FileMetadata metadata = fileService.getFileMetadata(fileId);
            byte[] data = fileService.downloadFile(fileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(metadata.getType()));
            headers.setContentDispositionFormData("attachment", metadata.getName());

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{fileId}")
    public ResponseEntity<?> updateFile(
            @PathVariable Long fileId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = authService.getUserIdFromToken(token);
            String username = authService.getUsernameFromToken(token);

            FileMetadata metadata = fileService.getFileMetadata(fileId);


            FileMetadata updatedMetadata = fileService.updateFile(fileId, file, userId, username);
            return ResponseEntity.ok(updatedMetadata);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(
            @PathVariable Long fileId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = getUserIdFromHeader(authHeader);
            FileMetadata metadata = fileService.getFileMetadata(fileId);

            if (!metadata.getUploaderId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            fileService.deleteFile(fileId);
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listFiles(
            @RequestParam(required = false) Boolean ascending,
            @RequestParam(required = false) List<String> types,
            @RequestHeader("Authorization") String authHeader) {
        try {
            List<FileMetadata> files;

            if (ascending != null) {
                files = sortFilterService.sortAndFilter(ascending, types);
            } else {
                files = fileService.listAllFiles();
                if (types != null && !types.isEmpty()) {
                    files = sortFilterService.filterByType(files, types);
                }
            }

            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<?> getFileMetadata(
            @PathVariable Long fileId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            FileMetadata metadata = fileService.getFileMetadata(fileId);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private MediaType getMediaType(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "kt":
                return MediaType.TEXT_PLAIN;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}