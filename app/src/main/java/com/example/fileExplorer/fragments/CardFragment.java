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

public class CardFragment extends BaseFileFragment {
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
        CardFragment internalFragment = new CardFragment();
        internalFragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, internalFragment)
                .addToBackStack("InternalFragment")
                .commit();
    }
}
