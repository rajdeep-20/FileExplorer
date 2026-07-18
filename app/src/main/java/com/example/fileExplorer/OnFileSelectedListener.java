package com.example.fileExplorer;

import java.io.File;

public interface OnFileSelectedListener {
    void onFileClick(File file);
    void onFileLongClick(File file, int position);
    
    // New methods for FileItem support
    void onFileItemClick(FileItem fileItem);
    void onFileItemLongClick(FileItem fileItem, int position);
}
