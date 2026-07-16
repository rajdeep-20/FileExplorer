package com.example.fileexplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.fileexplorer.FileItem;
import com.example.fileexplorer.R;

import java.io.File;

public class InternalFragment extends BaseFileFragment {
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
    protected String getTargetDirectoryPath() {
        return storage.getAbsolutePath();
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
                .addToBackStack("InternalFragment")
                .commit();
    }
}
