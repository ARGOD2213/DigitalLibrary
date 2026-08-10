package com.digitallibrary.dto;

import java.util.HashMap;
import java.util.Map;

public class ZipContents {

    private String coverFileKey;
    private String coverFileUrl;
    private String bookFileKey;
    private String bookFileUrl;
    private int extractedFilesCount;
    private Map<String, Object> metadata = new HashMap<>();

    public ZipContents() {}

    public ZipContents(String coverFileKey, String coverFileUrl, String bookFileKey, String bookFileUrl, int extractedFilesCount) {
        this.coverFileKey = coverFileKey;
        this.coverFileUrl = coverFileUrl;
        this.bookFileKey = bookFileKey;
        this.bookFileUrl = bookFileUrl;
        this.extractedFilesCount = extractedFilesCount;
    }

    public String getCoverFileKey() { return coverFileKey; }
    public void setCoverFileKey(String coverFileKey) { this.coverFileKey = coverFileKey; }

    public String getCoverFileUrl() { return coverFileUrl; }
    public void setCoverFileUrl(String coverFileUrl) { this.coverFileUrl = coverFileUrl; }

    public String getBookFileKey() { return bookFileKey; }
    public void setBookFileKey(String bookFileKey) { this.bookFileKey = bookFileKey; }

    public String getBookFileUrl() { return bookFileUrl; }
    public void setBookFileUrl(String bookFileUrl) { this.bookFileUrl = bookFileUrl; }

    public int getExtractedFilesCount() { return extractedFilesCount; }
    public void setExtractedFilesCount(int extractedFilesCount) { this.extractedFilesCount = extractedFilesCount; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
