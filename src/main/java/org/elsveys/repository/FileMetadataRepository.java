package org.elsveys.repository;

import org.elsveys.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByUploaderId(Long uploaderId);
    List<FileMetadata> findByTypeIn(List<String> types);
    List<FileMetadata> findAllByOrderByModifiedDateAsc();
    List<FileMetadata> findAllByOrderByModifiedDateDesc();
}