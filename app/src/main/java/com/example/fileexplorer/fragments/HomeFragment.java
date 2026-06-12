package com.example.fileexplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;

import com.example.fileexplorer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends BaseFIleFragment {
    private LinearLayout linearImage, linearVideo, linearDownloads, linearApks, linearMusic, linearDocs;

    @Override
    protected int getSourceLayoutResId() {
        return R.layout.fragment_home;
    }

    @Override
    protected void onViewCreateCustom(View view) {
        linearImage = view.findViewById(R.id.LinearImage);
        linearApks = view.findViewById(R.id.LinearApks);
        linearDocs = view.findViewById(R.id.LinearDocs);
        linearDownloads = view.findViewById(R.id.LinearDownloads);
        linearMusic = view.findViewById(R.id.LinearMusic);
        linearVideo = view.findViewById(R.id.LinearVideo);

        linearImage.setOnClickListener(v -> openCategory("images"));
        linearVideo.setOnClickListener(v -> openCategory("video"));
        linearMusic.setOnClickListener(v -> openCategory("music"));
        linearDownloads.setOnClickListener(v -> openCategory("downloads"));
        linearDocs.setOnClickListener(v -> openCategory("documents"));
        linearApks.setOnClickListener(v -> openCategory("APK"));
    }

    private void openCategory(String type) {
        Bundle args = new Bundle();
        args.putString("fileType", type);
        CategorizedFragment fragment = new CategorizedFragment();
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected List<File> getFilesToDisplay() {
        ArrayList<File> result = findFiles(Environment.getExternalStorageDirectory());
        result.sort(Comparator.comparing(File::lastModified).reversed());
        return result;
    }

    @Override
    protected int getRecyclerView() {
        return R.id.recyclerRecents;
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
                    arrayList.addAll(findFiles(singleFile));
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
