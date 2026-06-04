package com.example.fileexplorer.fragments;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fileexplorer.FileAdapter;
import com.example.fileexplorer.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InternalFragment extends Fragment {
    private RecyclerView recyclerView;
    private List<File> fileList;
    private ImageView img_back;
    private TextView tv_pathHolder;
    private FileAdapter fileAdapter;
    File storage;

    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_internal, container, false);
        tv_pathHolder = view.findViewById(R.id.tv_pathHolder);
        img_back = view.findViewById(R.id.imgBack);
        runtimePermission();

        String internalStorage = System.getenv("EXTERNAL_STORAGE");
        storage = new File(internalStorage);
        tv_pathHolder.setText(storage.getAbsolutePath());

        return view;
    }
    private void runtimePermission() {
        Dexter.withContext(getContext())
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        displayFiles();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    public ArrayList<File> findFiles(File file) {
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

    private void displayFiles() {

        // TODO: Implement file display logic
        recyclerView = view.findViewById(R.id.recycler_internal);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        fileList = new ArrayList<>();
        fileList.addAll(findFiles(storage));
        fileAdapter = new FileAdapter(getContext(), fileList);
        recyclerView.setAdapter(fileAdapter);

    }

}

