package com.example.fileexplorer.fragments;

import android.Manifest;
import android.app.Dialog;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fileexplorer.FileAdapter;
import com.example.fileexplorer.FileOpener;
import com.example.fileexplorer.OnFileSelectedListener;
import com.example.fileexplorer.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InternalFragment extends Fragment implements OnFileSelectedListener {
    private RecyclerView recyclerView;
    private List<File> fileList;
    private ImageView img_back;
    private TextView tv_pathHolder;
    private FileAdapter fileAdapter;
    String data;
    File storage;

    View view;
    String[] items = {"Details", " Rename", "Delete", "Share"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_internal, container, false);
        tv_pathHolder = view.findViewById(R.id.tv_pathHolder);
        img_back = view.findViewById(R.id.imgBack);

        // Environment.getExternalStorageDirectory() is the standard way to get internal storage
        storage = Environment.getExternalStorageDirectory();
        try {
            data = getArguments().getString("path");
            File file = new File(data);
            storage = file;
        } catch (Exception e) {
           e.printStackTrace();
        }
        tv_pathHolder.setText(storage.getAbsolutePath());

        runtimePermission();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                displayFiles();
            }
        }
    }

    private void runtimePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                displayFiles();
            } else {
                try {
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    android.net.Uri uri = android.net.Uri.fromParts("package", getContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } catch (Exception e) {
                    android.content.Intent intent = new android.content.Intent();
                    intent.setAction(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
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
        recyclerView = view.findViewById(R.id.recycler_internal);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        fileList = new ArrayList<>();

        // Running file scanning in a background thread to prevent UI lag
        new Thread(() -> {
            List<File> result = findFiles(storage);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    fileList.clear();
                    fileList.addAll(result);
                    fileAdapter = new FileAdapter(getContext(), fileList, this);
                    recyclerView.setAdapter(fileAdapter);
                });
            }
        }).start();
    }

    @Override
    public void onFileClick(File file) {

        if(file.isDirectory()){
            Bundle bundle = new Bundle();
            bundle.putString("path", file.getAbsolutePath());
            InternalFragment internalFragment = new InternalFragment();
            internalFragment.setArguments(bundle);
            getFragmentManager().beginTransaction().replace(R.id.fragment_container, internalFragment).addToBackStack(null).commit();
        }
        else {
            try {
                FileOpener.openFile(getContext(),file);
            } catch (IOException e) {
               e.printStackTrace();
            }
        }

    }

    @Override
    public void onFileLongClick(File file) {

        final Dialog optionDialoge = new Dialog(getContext());
        optionDialoge.setContentView(R.layout.option_diallogue);
        optionDialoge.setTitle("Select Option");
        ListView options = (ListView) optionDialoge.findViewById(R.id.List);
        CustomAdaptor customAdaptor = new CustomAdaptor();
        options.setAdapter(customAdaptor);
        options.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedItem = items[position];
            if (selectedItem.equals("Details")) {
                android.app.AlertDialog.Builder detailDialog = new android.app.AlertDialog.Builder(getContext());
                detailDialog.setTitle("Details");
                TextView detailText = new TextView(getContext());
                detailText.setPadding(50, 20, 50, 20);
                detailText.setText("File Name: " + file.getName() + "\n" +
                        "Size: " + android.text.format.Formatter.formatShortFileSize(getContext(), file.length()) + "\n" +
                        "Path: " + file.getAbsolutePath());
                detailDialog.setView(detailText);
                detailDialog.setPositiveButton("OK", null);
                detailDialog.show();
            } else if (selectedItem.equals(" Rename")) {
                android.widget.Toast.makeText(getContext(), "Rename not implemented yet", android.widget.Toast.LENGTH_SHORT).show();
            } else if (selectedItem.equals("Delete")) {
                if (file.delete()) {
                    android.widget.Toast.makeText(getContext(), "File deleted", android.widget.Toast.LENGTH_SHORT).show();
                    view.post(() -> displayFiles());
                } else {
                    android.widget.Toast.makeText(getContext(), "Cannot delete file", android.widget.Toast.LENGTH_SHORT).show();
                }
            } else if (selectedItem.equals("Share")) {
                try {
                    android.content.Intent shareIntent = new android.content.Intent();
                    shareIntent.setAction(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("*/*");
                    android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(getContext(), getContext().getApplicationContext().getPackageName() + ".provider", file);
                    shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(android.content.Intent.createChooser(shareIntent, "Share " + file.getName()));
                } catch (Exception e) {
                    android.widget.Toast.makeText(getContext(), "Cannot share this file", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            optionDialoge.dismiss();
        });
        optionDialoge.show();
    }


    class CustomAdaptor extends BaseAdapter{
        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int i) {
            return items[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View myView = getLayoutInflater().inflate(R.layout.option_layout,null);
            TextView textOption = myView.findViewById(R.id.txtOption);
            ImageView imgOption = myView.findViewById(R.id.imgOption);
            textOption.setText(items[position]);
            if(items[position].equals("Details"))
            {
                imgOption.setImageResource(R.drawable.ic_details);
            }
            else if (items[position].equals("Rename")){
                imgOption.setImageResource(R.drawable.ic_rename);
            }
            else if (items[position].equals("Delete")){
                imgOption.setImageResource(R.drawable.ic_delete);
            }
            else if (items[position].equals("Share")){
                imgOption.setImageResource(R.drawable.ic_share);
            }


            return myView;
        }
    }
}
