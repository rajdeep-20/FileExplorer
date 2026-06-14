package com.example.fileexplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;

import com.example.fileexplorer.FileAdapter;
import com.example.fileexplorer.FileItem;
import com.example.fileexplorer.R;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

        // For categorized search, we use a custom loading logic that searches recursively
        new Thread(() -> {
            List<FileItem> items = findFiles(path.toPath());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    fileList.clear();
                    fileList.addAll(items);
                    fileAdapter.notifyDataSetChanged();
                });
            }
        }).start();
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

    private List<FileItem> findFiles(Path dir) {
        List<FileItem> items = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    if (!entry.getFileName().toString().startsWith(".")) {
                        items.addAll(findFiles(entry));
                    }
                } else {
                    String name = entry.getFileName().toString().toLowerCase();
                    boolean shouldAdd = false;
                    if (fileType != null) {
                        switch (fileType) {
                            case "images":
                                shouldAdd = name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif");
                                break;
                            case "video":
                                shouldAdd = name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".3gp");
                                break;
                            case "music":
                                shouldAdd = name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".ogg");
                                break;
                            case "documents":
                                shouldAdd = name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") || name.endsWith(".pptx");
                                break;
                            case "APK":
                                shouldAdd = name.endsWith(".apk");
                                break;
                            case "downloads":
                                shouldAdd = true;
                                break;
                        }
                    }
                    if (shouldAdd) {
                        items.add(new FileItem(entry.getFileName().toString(), entry.toAbsolutePath().toString()));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return items;
    }
}
