package com.example.fileExplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;

import com.example.fileExplorer.FileAdapter;
import com.example.fileExplorer.FileItem;
import com.example.fileExplorer.FileLoadEngine;
import com.example.fileExplorer.FileTypeRegistry;
import com.example.fileExplorer.R;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class CategorizedFragment extends BaseFileFragment {

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
        if (fileType == null) return false;
        return FileTypeRegistry.matchesCategory(entry.getFileName().toString(), fileType);
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
        NavigableFragment child = NavigableFragment.newInstance(
                NavigableFragment.TYPE_INTERNAL, fileItem.getAbsolutePath());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, child)
                .addToBackStack("NavigableFragment")
                .commit();
    }
}
