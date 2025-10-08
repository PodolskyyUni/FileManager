package org.elsveys.model.dto;

public class FileUploadResponse {
    private Long fileId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String message;

    public FileUploadResponse() {}

    public FileUploadResponse(Long fileId, String fileName, String fileType, Long fileSize, String message) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.message = message;
    }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}