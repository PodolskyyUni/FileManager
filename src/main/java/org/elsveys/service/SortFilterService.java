package org.elsveys.service;

import org.elsveys.model.FileMetadata;
import org.elsveys.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SortFilterService {

    @Autowired
    private FileMetadataRepository fileRepository;

    public List<FileMetadata> sortByModifiedDate(boolean ascending) {
        if (ascending) {
            return fileRepository.findAllByOrderByModifiedDateAsc();
        } else {
            return fileRepository.findAllByOrderByModifiedDateDesc();
        }
    }

    public List<FileMetadata> filterByType(List<FileMetadata> files, List<String> types) {
        if (types == null || types.isEmpty()) {
            return files;
        }

        return files.stream()
                .filter(f -> types.contains(f.getType()))
                .collect(Collectors.toList());
    }

    public List<FileMetadata> sortAndFilter(boolean ascending, List<String> types) {
        List<FileMetadata> sorted = sortByModifiedDate(ascending);

        if (types != null && !types.isEmpty()) {
            return filterByType(sorted, types);
        }

        return sorted;
    }

    public List<FileMetadata> getAllFilesForUser(Long userId) {
        return fileRepository.findByUploaderId(userId);
    }
}