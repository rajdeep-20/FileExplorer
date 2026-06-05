package com.example.fileexplorer;

import java.io.File;

public interface OnFileSelectedListener {
    void onFileClick(File file);
    void onFileLongClick(File file);
}
