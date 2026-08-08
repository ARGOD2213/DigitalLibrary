package com.digitallibrary.storage;

public class StoredFile {

    private final String fileName;
    private final String fileUrl;

    public StoredFile(String fileName, String fileUrl) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }
}
