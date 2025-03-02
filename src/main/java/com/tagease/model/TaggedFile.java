package com.tagease.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class TaggedFile {
    private String fileName;
    private String filePath;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private Set<TaggedFile> relatedFiles;

    public TaggedFile(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.tags = new HashSet<>();
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.relatedFiles = new HashSet<>();
    }

    public TaggedFile(String fileName, String filePath, Set<String> tags) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.tags = tags != null ? tags : new HashSet<>();
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.relatedFiles = new HashSet<>();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void removeTag(String tag) {
        this.tags.remove(tag);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Set<TaggedFile> getRelatedFiles() {
        return relatedFiles;
    }

    public void setRelatedFiles(Set<TaggedFile> relatedFiles) {
        this.relatedFiles = relatedFiles;
    }

    public void addRelatedFile(TaggedFile file) {
        if (relatedFiles == null) {
            relatedFiles = new HashSet<>();
        }
        if (file != null && !file.getFilePath().equals(this.filePath)) {
            relatedFiles.add(file);
        }
    }

    public void removeRelatedFile(TaggedFile file) {
        if (relatedFiles != null) {
            relatedFiles.remove(file);
        }
    }

    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaggedFile that = (TaggedFile) o;
        return filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }

    @Override
    public String toString() {
        return fileName + " (" + tags.size() + " tags)";
    }
}
