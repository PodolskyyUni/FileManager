package org.elsveys.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elsveys.contoller.SyncController;
import org.elsveys.model.FileMetadata;
import org.elsveys.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private FileService fileService;

    @InjectMocks
    private SyncController syncController;

    private FileMetadata file1;
    private FileMetadata file2;
    private FileMetadata file3;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(syncController).build();
        objectMapper = new ObjectMapper();

        file1 = new FileMetadata();
        file1.setFileId(1L);
        file1.setName("user1_file.kt");
        file1.setType("kt");
        file1.setUploaderId(1L);
        file1.setUploaderName("user1");

        file2 = new FileMetadata();
        file2.setFileId(2L);
        file2.setName("user2_file.jpg");
        file2.setType("jpg");
        file2.setUploaderId(2L);
        file2.setUploaderName("user2");

        file3 = new FileMetadata();
        file3.setFileId(3L);
        file3.setName("user2_another.kt");
        file3.setType("kt");
        file3.setUploaderId(2L);
        file3.setUploaderName("user2");
    }

    @Test
    void testGetRemoteFilesReturnsAllFiles() throws Exception {
        List<FileMetadata> allFiles = Arrays.asList(file1, file2, file3);
        when(fileService.listAllFiles()).thenReturn(allFiles);

        mockMvc.perform(get("/api/sync/remote-files")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("user1_file.kt"))
                .andExpect(jsonPath("$[1].name").value("user2_file.jpg"))
                .andExpect(jsonPath("$[2].name").value("user2_another.kt"));
    }

    @Test
    void testCompareFilesWithSharedWorkspace() throws Exception {
        List<FileMetadata> allFiles = Arrays.asList(file1, file2, file3);
        when(fileService.listAllFiles()).thenReturn(allFiles);

        Map<String, Object> payload = new HashMap<>();
        payload.put("localFiles", Arrays.asList("user1_file.kt", "local_only.kt"));

        mockMvc.perform(post("/api/sync/compare")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toUpload.length()").value(1))
                .andExpect(jsonPath("$.toUpload[0]").value("local_only.kt"))
                .andExpect(jsonPath("$.toDownload.length()").value(2))
                .andExpect(jsonPath("$.toDownload[0]").value("user2_file.jpg"))
                .andExpect(jsonPath("$.toDownload[1]").value("user2_another.kt"));
    }

    @Test
    void testCompareFilesAllInSync() throws Exception {
        List<FileMetadata> allFiles = Arrays.asList(file1, file2);
        when(fileService.listAllFiles()).thenReturn(allFiles);

        Map<String, Object> payload = new HashMap<>();
        payload.put("localFiles", Arrays.asList("user1_file.kt", "user2_file.jpg"));

        mockMvc.perform(post("/api/sync/compare")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toUpload.length()").value(0))
                .andExpect(jsonPath("$.toDownload.length()").value(0));
    }

    @Test
    void testCompareFilesOnlyLocalFiles() throws Exception {
        when(fileService.listAllFiles()).thenReturn(Arrays.asList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("localFiles", Arrays.asList("local1.kt", "local2.jpg"));

        mockMvc.perform(post("/api/sync/compare")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toUpload.length()").value(2))
                .andExpect(jsonPath("$.toDownload.length()").value(0));
    }

    @Test
    void testCompareFilesOnlyRemoteFiles() throws Exception {
        List<FileMetadata> allFiles = Arrays.asList(file1, file2);
        when(fileService.listAllFiles()).thenReturn(allFiles);

        Map<String, Object> payload = new HashMap<>();
        payload.put("localFiles", Arrays.asList());

        mockMvc.perform(post("/api/sync/compare")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toUpload.length()").value(0))
                .andExpect(jsonPath("$.toDownload.length()").value(2));
    }
}