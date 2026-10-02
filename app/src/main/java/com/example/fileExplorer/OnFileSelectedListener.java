package com.example.fileExplorer;

public interface OnFileSelectedListener {
    void onFileItemClick(FileItem fileItem);
    void onFileItemLongClick(FileItem fileItem, int position);
}
