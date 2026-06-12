package com.example.fileexplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fileexplorer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CardFragment extends BaseFIleFragment {
    private ImageView img_back;
    private TextView tv_pathHolder;
    private File storage;

    @Override
    protected int getSourceLayoutResId() {
        return R.layout.fragment_card;
    }

    @Override
    protected void onViewCreateCustom(View view) {
        tv_pathHolder = view.findViewById(R.id.tv_pathHolder);
        img_back = view.findViewById(R.id.imgBack);

        File sdCard = getSDCard();
        if (sdCard == null) {
            Toast.makeText(getContext(), "SD Card not found!", Toast.LENGTH_SHORT).show();
            storage = new File("/");
            tv_pathHolder.setText("No SD Card");
            return;
        }

        storage = sdCard;
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

    private File getSDCard() {
        File[] externalFilesDirs = requireContext().getExternalFilesDirs(null);
        for (File file : externalFilesDirs) {
            if (file != null && Environment.isExternalStorageRemovable(file)) {
                // The path returned by getExternalFilesDirs is usually /storage/XXXX-XXXX/Android/data/com.example...
                // We want the root of the SD card: /storage/XXXX-XXXX/
                String path = file.getAbsolutePath();
                int index = path.indexOf("/Android");
                if (index != -1) {
                    return new File(path.substring(0, index));
                }
            }
        }
        return null;
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
        CardFragment internalFragment = new CardFragment();
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
