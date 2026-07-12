package com.example.fileexplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;

import com.example.fileexplorer.FileAdapter;
import com.example.fileexplorer.FileItem;
import com.example.fileexplorer.FileLoadEngine;
import com.example.fileexplorer.R;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

public class CategorizedFragment extends BaseFIleFragment {

    private File path;
    private String fileType;

    @Override
    protected int getSourceLayoutResId() {
        return R.layout.fragment_categorized;
    }

    @Override
    protected void onViewCreateCustom(View view) {
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            fileType = bundle.getString("fileType");
            if (fileType != null && fileType.equals("downloads")) {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            } else {
                path = Environment.getExternalStorageDirectory();
            }
        } else {
            path = Environment.getExternalStorageDirectory();
        }
    }

    @Override
    protected void displayFiles() {
        recyclerView = view.findViewById(getRecyclerView());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        fileAdapter = new FileAdapter(getContext(), fileList, this);
        recyclerView.setAdapter(fileAdapter);

        fileList.clear();
        fileAdapter.notifyDataSetChanged();

        fileLoadEngine.loadRecursive(path.getAbsolutePath(), this::filterFile, new FileLoadEngine.FileLoadListener() {
            @Override
            public void onStructureLoaded(List<FileItem> items) {
                // Not used in recursive load
            }

            @Override
            public void onItemsAdded(List<FileItem> newItems) {
                if (getActivity() != null) {
                    int startPos = fileList.size();
                    fileList.addAll(newItems);
                    fileAdapter.notifyItemRangeInserted(startPos, newItems.size());
                }
            }

            @Override
            public void onItemMetadataUpdated(int position, FileItem updatedItem) {
                fileAdapter.notifyItemChanged(position);
            }
        });
    }

    private boolean filterFile(Path entry) {
        String name = entry.getFileName().toString().toLowerCase();
        if (fileType != null) {
            switch (fileType) {
                case "images":
                    return name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif");
                case "video":
                    return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".3gp");
                case "music":
                    return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".ogg");
                case "documents":
                    return name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") || name.endsWith(".pptx");
                case "APK":
                    return name.endsWith(".apk");
                case "downloads":
                    return true;
            }
        }
        return false;
    }

    @Override
    protected String getTargetDirectoryPath() {
        return path.getAbsolutePath();
    }

    @Override
    protected int getRecyclerView() {
        return R.id.recycler_internal;
    }

    @Override
    protected void openDirectory(FileItem fileItem) {
        Bundle bundle = new Bundle();
        bundle.putString("path", fileItem.getAbsolutePath());
        InternalFragment internalFragment = new InternalFragment();
        internalFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, internalFragment)
                .addToBackStack("InteranlFragment")
                .commit();
    }
}
