package com.example.fileexplorer;


import java.util.Objects;

/**
 * The Lightweight UI Data Model
 * This class holds only the data needed for drawing the screen.
 * It separates the initial display state from the background metadata load.
 */
public class FileItem {
    private final String name;
    private final String absolutePath;
    private boolean isDirectory;
    private boolean isMetadataLoaded;
    private long fileSize;
    private long lastModified;

    // Constructor for the instant "Skeleton" load
    public FileItem(String name, String absolutePath) {
        this.name = name;
        this.absolutePath = absolutePath;
        this.isDirectory = false; // Default placeholder
        this.isMetadataLoaded = false;
        this.fileSize = 0;
        this.lastModified = 0;
    }

    // Getters and Setters
    public String getName() { return name; }
    public String getAbsolutePath() { return absolutePath; }
    public boolean isDirectory() { return isDirectory; }
    public boolean isMetadataLoaded() { return isMetadataLoaded; }
    public long getFileSize() { return fileSize; }
    public long getLastModified() { return lastModified; }

    public void updateMetadata(boolean isDirectory, long fileSize, long lastModified) {
        this.isDirectory = isDirectory;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.isMetadataLoaded = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileItem fileItem = (FileItem) o;
        return Objects.equals(absolutePath, fileItem.absolutePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(absolutePath);
    }
}
