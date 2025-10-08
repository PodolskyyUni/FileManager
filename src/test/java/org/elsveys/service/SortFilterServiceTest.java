package org.elsveys.service;

import org.elsveys.model.FileMetadata;
import org.elsveys.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SortFilterServiceTest {

    @Mock
    private FileMetadataRepository fileRepository;

    @InjectMocks
    private SortFilterService sortFilterService;

    private FileMetadata file1;
    private FileMetadata file2;
    private FileMetadata file3;

    @BeforeEach
    void setUp() {
        file1 = new FileMetadata();
        file1.setFileId(1L);
        file1.setName("test1.kt");
        file1.setType("kt");
        file1.setModifiedDate(LocalDateTime.of(2025, 1, 1, 10, 0));

        file2 = new FileMetadata();
        file2.setFileId(2L);
        file2.setName("test2.jpg");
        file2.setType("jpg");
        file2.setModifiedDate(LocalDateTime.of(2025, 1, 2, 10, 0));

        file3 = new FileMetadata();
        file3.setFileId(3L);
        file3.setName("test3.kt");
        file3.setType("kt");
        file3.setModifiedDate(LocalDateTime.of(2025, 1, 3, 10, 0));
    }

    @Test
    void testSortByModifiedDateAscending() {
        List<FileMetadata> expectedAscending = Arrays.asList(file1, file2, file3);
        when(fileRepository.findAllByOrderByModifiedDateAsc()).thenReturn(expectedAscending);

        List<FileMetadata> result = sortFilterService.sortByModifiedDate(true);

        assertEquals(3, result.size());
        assertEquals("test1.kt", result.get(0).getName());
        assertEquals("test2.jpg", result.get(1).getName());
        assertEquals("test3.kt", result.get(2).getName());
        verify(fileRepository, times(1)).findAllByOrderByModifiedDateAsc();
    }

    @Test
    void testSortByModifiedDateDescending() {
        List<FileMetadata> expectedDescending = Arrays.asList(file3, file2, file1);
        when(fileRepository.findAllByOrderByModifiedDateDesc()).thenReturn(expectedDescending);

        List<FileMetadata> result = sortFilterService.sortByModifiedDate(false);

        assertEquals(3, result.size());
        assertEquals("test3.kt", result.get(0).getName());
        assertEquals("test2.jpg", result.get(1).getName());
        assertEquals("test1.kt", result.get(2).getName());
        verify(fileRepository, times(1)).findAllByOrderByModifiedDateDesc();
    }

    @Test
    void testFilterByType() {
        List<FileMetadata> allFiles = Arrays.asList(file1, file2, file3);
        List<String> typesToFilter = Arrays.asList("kt");

        List<FileMetadata> result = sortFilterService.filterByType(allFiles, typesToFilter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(f -> f.getType().equals("kt")));
    }

    @Test
    void testFilterByTypeWithMultipleTypes() {
        List<FileMetadata> allFiles = Arrays.asList(file1, file2, file3);
        List<String> typesToFilter = Arrays.asList("kt", "jpg");

        List<FileMetadata> result = sortFilterService.filterByType(allFiles, typesToFilter);

        assertEquals(3, result.size());
    }

    @Test
    void testFilterByTypeEmptyList() {
        List<FileMetadata> allFiles = Arrays.asList(file1, file2, file3);

        List<FileMetadata> result = sortFilterService.filterByType(allFiles, null);

        assertEquals(3, result.size());
        assertEquals(allFiles, result);
    }

    @Test
    void testSortAndFilterCombined() {
        List<FileMetadata> sortedFiles = Arrays.asList(file1, file2, file3);
        when(fileRepository.findAllByOrderByModifiedDateAsc()).thenReturn(sortedFiles);

        List<String> typesToFilter = Arrays.asList("kt");
        List<FileMetadata> result = sortFilterService.sortAndFilter(true, typesToFilter);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(f -> f.getType().equals("kt")));
    }

    @Test
    void testGetAllFilesForUser() {
        Long userId = 1L;
        List<FileMetadata> userFiles = Arrays.asList(file1, file2);
        when(fileRepository.findByUploaderId(userId)).thenReturn(userFiles);

        List<FileMetadata> result = sortFilterService.getAllFilesForUser(userId);

        assertEquals(2, result.size());
        verify(fileRepository, times(1)).findByUploaderId(userId);
    }
}