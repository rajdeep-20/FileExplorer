package com.example.fileExplorer.fragments;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fileExplorer.FileItem;
import com.example.fileExplorer.R;

import java.io.File;

/**
 * Unified navigable directory fragment that replaces both InternalFragment and CardFragment.
 * Pass "storageType" = "internal" or "sdcard" to control the root, and "path" to open a subdirectory.
 */
public class NavigableFragment extends BaseFileFragment {

    public static final String ARG_STORAGE_TYPE = "storageType";
    public static final String ARG_PATH = "path";
    public static final String TYPE_INTERNAL = "internal";
    public static final String TYPE_SDCARD = "sdcard";

    private TextView tvPathHolder;
    private File storage;

    /** Factory to create with the right storage type and optional path. */
    public static NavigableFragment newInstance(String storageType, String path) {
        NavigableFragment frag = new NavigableFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STORAGE_TYPE, storageType);
        if (path != null) args.putString(ARG_PATH, path);
        frag.setArguments(args);
        return frag;
    }

    public static NavigableFragment newInstance(String storageType) {
        return newInstance(storageType, null);
    }

    @Override
    protected int getSourceLayoutResId() {
        return R.layout.fragment_internal;
    }

    @Override
    protected void onViewCreateCustom(View view) {
        tvPathHolder = view.findViewById(R.id.tv_pathHolder);
        ImageView imgBack = view.findViewById(R.id.imgBack);

        String storageType = TYPE_INTERNAL;
        if (getArguments() != null) {
            storageType = getArguments().getString(ARG_STORAGE_TYPE, TYPE_INTERNAL);
        }

        // Resolve default root based on storage type
        if (TYPE_SDCARD.equals(storageType)) {
            File sdCard = getSDCard();
            if (sdCard == null) {
                Toast.makeText(getContext(), "SD Card not found!", Toast.LENGTH_SHORT).show();
                storage = new File("/");
                tvPathHolder.setText("No SD Card");
                return;
            }
            storage = sdCard;
        } else {
            storage = Environment.getExternalStorageDirectory();
        }

        // Override with explicit path argument if provided
        try {
            if (getArguments() != null) {
                String path = getArguments().getString(ARG_PATH);
                if (path != null) {
                    storage = new File(path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvPathHolder.setText(storage.getAbsolutePath());
    }

    private File getSDCard() {
        File[] externalFilesDirs = requireContext().getExternalFilesDirs(null);
        for (File file : externalFilesDirs) {
            if (file != null && Environment.isExternalStorageRemovable(file)) {
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
    protected String getTargetDirectoryPath() {
        return storage.getAbsolutePath();
    }

    @Override
    protected int getRecyclerView() {
        return R.id.recycler_internal;
    }

    @Override
    protected void openDirectory(FileItem fileItem) {
        // Preserve the same storage type when navigating into subdirectories
        String storageType = TYPE_INTERNAL;
        if (getArguments() != null) {
            storageType = getArguments().getString(ARG_STORAGE_TYPE, TYPE_INTERNAL);
        }
        NavigableFragment child = NavigableFragment.newInstance(storageType, fileItem.getAbsolutePath());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, child)
                .addToBackStack("NavigableFragment")
                .commit();
    }
}
