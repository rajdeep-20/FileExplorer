package com.example.fileexplorer.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CardFragment extends Fragment implements OnFileSelectedListener {
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
        view = inflater.inflate(R.layout.fragment_card, container, false);
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
            if (Environment.isExternalStorageManager()) {
                displayFiles();
            }
        }
    }

    private void runtimePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                displayFiles();
            } else {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
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

        if (file.isDirectory()) {
            Bundle bundle = new Bundle();
            bundle.putString("path", file.getAbsolutePath());
            CardFragment internalFragment = new CardFragment();
            internalFragment.setArguments(bundle);
            getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, internalFragment).addToBackStack(null).commit();
        } else {
            try {
                FileOpener.openFile(getContext(), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onFileLongClick(File file, int filePosition) {

        final Dialog optionDialoge = new Dialog(getContext());
        optionDialoge.setContentView(R.layout.option_diallogue);
        optionDialoge.setTitle("Select Option");
        ListView options = optionDialoge.findViewById(R.id.List);
        CustomAdaptor customAdaptor = new CustomAdaptor();
        options.setAdapter(customAdaptor);
        options.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedItem = items[position];
            if (selectedItem.equals("Details")) {
                AlertDialog.Builder detailDialog = new AlertDialog.Builder(getContext());
                detailDialog.setTitle("Details");
                Date lastmodified = new Date(file.lastModified());
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                String formattedDate = formatter.format(lastmodified);
                TextView detailText = new TextView(getContext());
                detailText.setPadding(50, 20, 50, 20);
                detailText.setText("File Name: " + file.getName() + "\n" +
                        "Size: " + Formatter.formatShortFileSize(getContext(), file.length()) + "\n" +
                        "Path: " + file.getAbsolutePath() + "\n" +
                        "Last Modified:" + formattedDate);
                detailDialog.setView(detailText);
                detailDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        optionDialoge.cancel();
                    }
                });
                AlertDialog alertDialog = detailDialog.create();
                alertDialog.show();
            } else if (selectedItem.equals(" Rename")) {
                AlertDialog.Builder renameAlert = new AlertDialog.Builder(getContext());
                renameAlert.setTitle("Rename File");


                final EditText name = new EditText(getContext());
                name.setText(file.getName());
                renameAlert.setView(name);

                renameAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = name.getEditableText().toString();
                        String extension = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("."));
                        File destination = new File(file.getParent(), newName + extension);

                        if (file.renameTo(destination)) {
                            fileList.set(filePosition, destination);
                            fileAdapter.notifyItemChanged(filePosition);
                            Toast.makeText(getContext(), "Renamed!!", Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(getContext(), "Couldn't Rename File", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                renameAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        optionDialoge.cancel();
                    }
                });
                renameAlert.show();
            } else if (selectedItem.equals("Delete")) {
                AlertDialog.Builder deleteDialoge = new AlertDialog.Builder(getContext());
                deleteDialoge.setTitle("Delete " + file.getName() + "?");
                deleteDialoge.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (file.delete()) {
                            fileList.remove(filePosition);
                            fileAdapter.notifyDataSetChanged();
                            Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                deleteDialoge.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        optionDialoge.cancel();
                    }
                });
                deleteDialoge.show();
            } else if (selectedItem.equals("Share")) {
                try {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.setType("*/*");
                    Uri uri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext().getPackageName() + ".provider", file);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share " + file.getName()));
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Cannot share this file", Toast.LENGTH_SHORT).show();
                }
            }
            optionDialoge.dismiss();
        });
        optionDialoge.show();
    }


    class CustomAdaptor extends BaseAdapter {
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
            View myView = getLayoutInflater().inflate(R.layout.option_layout, null);
            TextView textOption = myView.findViewById(R.id.txtOption);
            ImageView imgOption = myView.findViewById(R.id.imgOption);
            textOption.setText(items[position]);
            if (items[position].equals("Details")) {
                imgOption.setImageResource(R.drawable.ic_details);
            } else if (items[position].equals("Rename")) {
                imgOption.setImageResource(R.drawable.ic_rename);
            } else if (items[position].equals("Delete")) {
                imgOption.setImageResource(R.drawable.ic_delete);
            } else if (items[position].equals("Share")) {
                imgOption.setImageResource(R.drawable.ic_share);
            }


            return myView;
        }
    }
}
