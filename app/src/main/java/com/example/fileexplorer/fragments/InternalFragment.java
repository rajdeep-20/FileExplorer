package com.example.fileexplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fileexplorer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InternalFragment extends BaseFIleFragment {
    private ImageView img_back;
    private TextView tv_pathHolder;
    private File storage;

    @Override
    protected int getSourceLayoutResId() {
        return R.layout.fragment_internal;
    }

    @Override
    protected void onViewCreateCustom(View view) {
        tv_pathHolder = view.findViewById(R.id.tv_pathHolder);
        img_back = view.findViewById(R.id.imgBack);

        storage = Environment.getExternalStorageDirectory();
        try {
            if (getArguments() != null) {
                String path = getArguments().getString("path");
                if (path != null) {
                    storage = new File(path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        tv_pathHolder.setText(storage.getAbsolutePath());
    }

    @Override
    protected List<File> getFilesToDisplay() {
        return findFiles(storage);
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
                .addToBackStack("InternalFragment")
                .commit();
    }

    private ArrayList<File> findFiles(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();

        if (files != null) {
            for (File singleFile : files) {
                if (singleFile.isDirectory() && !singleFile.isHidden()) {
                    arrayList.add(singleFile);
                } else {
                    String name = singleFile.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                            name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".apk") ||
                            name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".txt") ||
                            name.endsWith(".mp4") || name.endsWith(".raw") || name.endsWith(".dng")) {
                        arrayList.add(singleFile);
                    }
                }
            }
        }
        return arrayList;
    }
}
