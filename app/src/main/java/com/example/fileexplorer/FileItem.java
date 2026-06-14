package com.example.fileexplorer;



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

    // Constructor for the instant "Skeleton" load
    public FileItem(String name, String absolutePath) {
        this.name = name;
        this.absolutePath = absolutePath;
        this.isDirectory = false; // Default placeholder
        this.isMetadataLoaded = false;
        this.fileSize = 0;
    }

    // Getters and Setters
    public String getName() { return name; }
    public String getAbsolutePath() { return absolutePath; }
    public boolean isDirectory() { return isDirectory; }
    public boolean isMetadataLoaded() { return isMetadataLoaded; }
    public long getFileSize() { return fileSize; }

    public void updateMetadata(boolean isDirectory, long fileSize) {
        this.isDirectory = isDirectory;
        this.fileSize = fileSize;
        this.isMetadataLoaded = true;
    }
}
