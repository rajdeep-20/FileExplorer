package com.example.fileExplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.LinearLayout;

import com.example.fileExplorer.FileItem;
import com.example.fileExplorer.R;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseFileFragment {
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
    protected void displayFiles() {
        super.displayFiles(); // This will load recents from getTargetDirectoryPath()
        // But for Home, we might want to override to search everywhere for "Recents"
        // For now, let's keep it simple and just load from ExternalStorageDirectory
    }

    @Override
    protected String getTargetDirectoryPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    @Override
    protected int getRecyclerView() {
        return R.id.recyclerRecents;
    }

    @Override
    protected void openDirectory(FileItem fileItem) {
        Bundle bundle = new Bundle();
        bundle.putString("path", fileItem.getAbsolutePath());
        InternalFragment internalFragment = new InternalFragment();
        internalFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, internalFragment)
                .addToBackStack("InternalFragment")
                .commit();
    }
}
