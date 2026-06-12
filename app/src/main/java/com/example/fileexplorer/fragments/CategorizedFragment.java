package com.example.fileexplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.example.fileexplorer.R;

import java.io.File;
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
    protected List<File> getFilesToDisplay() {
        return findFiles(path);
    }

    @Override
    protected int getRecyclerView() {
        return R.id.recycler_internal;
    }

    @Override
    protected void openDirectory(File file) {
        Bundle bundle = new Bundle();
        bundle.putString("path", file.getAbsolutePath());
        InternalFragment internalFragment = new InternalFragment();
        internalFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, internalFragment)
                .addToBackStack("InteranlFragment")
                .commit();
    }

    private ArrayList<File> findFiles(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();

        if (files != null) {
            for (File singleFile : files) {
                if (singleFile.isDirectory() && !singleFile.isHidden()) {
                    arrayList.addAll(findFiles(singleFile));
                } else {
                    String name = singleFile.getName().toLowerCase();
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
                        arrayList.add(singleFile);
                    }
                }
            }
        }
        return arrayList;
    }
}
