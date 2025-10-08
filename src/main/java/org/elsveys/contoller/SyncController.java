package org.elsveys.contoller;

import org.elsveys.model.FileMetadata;
import org.elsveys.service.AuthService;
import org.elsveys.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    @Autowired
    private FileService fileService;

    @Autowired
    private AuthService authService;

    @PostMapping("/compare")
    public ResponseEntity<?> compareFiles(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("Authorization") String authHeader) {
        try {
            @SuppressWarnings("unchecked")
            List<String> localFiles = (List<String>) payload.get("localFiles");

            List<FileMetadata> allRemoteFiles = fileService.listAllFiles();

            List<String> remoteFileNames = allRemoteFiles.stream()
                    .map(FileMetadata::getName)
                    .collect(Collectors.toList());

            List<String> toUpload = localFiles.stream()
                    .filter(name -> !remoteFileNames.contains(name))
                    .collect(Collectors.toList());

            List<String> toDownload = remoteFileNames.stream()
                    .filter(name -> !localFiles.contains(name))
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("toUpload", toUpload);
            result.put("toDownload", toDownload);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/remote-files")
    public ResponseEntity<?> getRemoteFiles(@RequestHeader("Authorization") String authHeader) {
        try {
            List<FileMetadata> files = fileService.listAllFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}