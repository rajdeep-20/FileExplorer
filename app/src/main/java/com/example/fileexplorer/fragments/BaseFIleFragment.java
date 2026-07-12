package com.example.fileexplorer.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fileexplorer.FileAdapter;
import com.example.fileexplorer.FileItem;
import com.example.fileexplorer.FileLoadEngine;
import com.example.fileexplorer.FileOpener;
import com.example.fileexplorer.OnFileSelectedListener;
import com.example.fileexplorer.R;
import com.example.fileexplorer.ScrollBarInterface;
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

public abstract class BaseFIleFragment extends Fragment implements OnFileSelectedListener, ScrollBarInterface {
    protected RecyclerView recyclerView;
    protected List<FileItem> fileList = new ArrayList<>();
    protected FileAdapter fileAdapter;
    protected FileLoadEngine fileLoadEngine = new FileLoadEngine();

    protected View view;
    protected String[] items = {"Details", "Rename", "Delete", "Share"};
    protected SortingOrder.sortingOrder currentSortOrder = SortingOrder.sortingOrder.TIME_DESC;

    @Override
    public void setupScrollBar(RecyclerView recyclerView) {
        // Handled in XML
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(getSourceLayoutResId(), container, false);
        onViewCreateCustom(view);
        runTimePermission();
        return view;
    }

    protected abstract int getSourceLayoutResId();

    protected abstract void onViewCreateCustom(View view);

    protected abstract String getTargetDirectoryPath();

    protected abstract int getRecyclerView();

    protected abstract void openDirectory(FileItem fileItem);

    protected void runTimePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                displayFiles();
            } else {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            Dexter.withContext(getContext())
                    .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            displayFiles();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();
        }
    }

    protected void displayFiles() {
        recyclerView = view.findViewById(getRecyclerView());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        fileAdapter = new FileAdapter(getContext(), fileList, this);
        recyclerView.setAdapter(fileAdapter);

        fileLoadEngine.loadDirectory(getTargetDirectoryPath(), new FileLoadEngine.FileLoadListener() {
            @Override
            public void onStructureLoaded(List<FileItem> items) {
                fileList.clear();
                fileList.addAll(items);
                if (!fileList.isEmpty()) {
                    fileList.sort(currentSortOrder.getComparator());
                }
                fileAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemsAdded(List<FileItem> newItems) {
                int startPos = fileList.size();
                fileList.addAll(newItems);
                fileAdapter.notifyItemRangeInserted(startPos, newItems.size());
            }

            @Override
            public void onItemMetadataUpdated(int position, FileItem updatedItem) {
                fileAdapter.notifyItemChanged(position);
            }
        });
    }

    @Override
    public void onFileClick(File file) {
        // Deprecated, use onFileItemClick
    }

    @Override
    public void onFileLongClick(File file, int position) {
        // Deprecated, use onFileItemLongClick
    }

    @Override
    public void onFileItemClick(FileItem fileItem) {
        if (fileItem.isDirectory()) {
            openDirectory(fileItem);
        } else {
            try {
                FileOpener.openFile(getContext(), new File(fileItem.getAbsolutePath()));
            } catch (IOException e) {
                Toast.makeText(getContext(), "Error opening File", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("SetText18n")
    @Override
    public void onFileItemLongClick(FileItem fileItem, int position) {
        final Dialog optionalDialog = new Dialog(getContext());
        optionalDialog.setContentView(R.layout.option_diallogue);
        ListView listView = optionalDialog.findViewById(R.id.List);
        listView.setAdapter(new CustomAdpator());
        listView.setOnItemClickListener((parent, v, pos, id) -> {
            handleOptionClick(items[pos], fileItem, optionalDialog);
        });
        optionalDialog.show();
    }

    private void handleOptionClick(String selectedItem, FileItem fileItem, Dialog dialog) {
        File file = new File(fileItem.getAbsolutePath());
        if (selectedItem.equals("Details")) {
            showDetails(fileItem);
        } else if (selectedItem.equals("Rename")) {
            showRenameDailog(fileItem);
        } else if (selectedItem.equals("Share")) {
            shareFile(file);
        } else if (selectedItem.equals("Delete")) {
            showDeletedDialog(fileItem);
        }
        dialog.dismiss();
    }

    private void showDeletedDialog(FileItem fileItem) {
        File file = new File(fileItem.getAbsolutePath());
        new AlertDialog.Builder(getContext())
                .setTitle("Delete")
                .setMessage("Delete " + fileItem.getName() + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (file.delete()) {
                        int currentPos = fileList.indexOf(fileItem);
                        if (currentPos != -1) {
                            fileList.remove(currentPos);
                            fileAdapter.notifyItemRemoved(currentPos);
                            Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showDetails(FileItem fileItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Details");
        File file = new File(fileItem.getAbsolutePath());
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(file.lastModified()));
        builder.setMessage("Name: " + fileItem.getName() + "\nSize: " + Formatter.formatShortFileSize(getContext(), fileItem.getFileSize()) + "\nPath: " + fileItem.getAbsolutePath() + "\nModified: " + date);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showRenameDailog(FileItem fileItem) {
        AlertDialog.Builder rename = new AlertDialog.Builder(getContext());
        rename.setTitle("Rename");
        EditText input = new EditText(getContext());
        input.setText(fileItem.getName());
        rename.setView(input);
        rename.setPositiveButton("OK", (d, w) -> {
            File file = new File(fileItem.getAbsolutePath());
            String newName = input.getText().toString();
            File dest = new File(file.getParent(), newName);

            if (file.renameTo(dest)) {
                int currentPos = fileList.indexOf(fileItem);
                if (currentPos != -1) {
                    FileItem updatedItem = new FileItem(newName, dest.getAbsolutePath());
                    updatedItem.updateMetadata(fileItem.isDirectory(), fileItem.getFileSize(), fileItem.getLastModified());
                    fileList.set(currentPos, updatedItem);
                    fileAdapter.notifyItemChanged(currentPos);
                    Toast.makeText(getContext(), "Renamed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rename.setNegativeButton("Cancel", null);
        rename.show();
    }

    private void shareFile(File file) {
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("*/*");
            Uri link = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
            share.putExtra(Intent.EXTRA_STREAM, link);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Cannot Share", Toast.LENGTH_SHORT).show();
        }
    }

    public void sortFiles(SortingOrder.sortingOrder sortOrder) {
        this.currentSortOrder = sortOrder;
        if (!fileList.isEmpty()) {
            fileList.sort(sortOrder.getComparator());
            fileAdapter.notifyDataSetChanged();
        }
    }

    class CustomAdpator extends BaseAdapter {
        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = getLayoutInflater().inflate(R.layout.option_layout, null);
            TextView textView = view.findViewById(R.id.txtOption);
            ImageView imageView = view.findViewById(R.id.imgOption);
            textView.setText(items[position]);
            if (items[position].equals("Details"))
                imageView.setImageResource(R.drawable.ic_details);
            else if (items[position].equals("Rename"))
                imageView.setImageResource(R.drawable.ic_rename);
            else if (items[position].equals("Delete"))
                imageView.setImageResource(R.drawable.ic_delete);
            else if (items[position].equals("Share"))
                imageView.setImageResource(R.drawable.ic_share);
            return view;
        }
    }
}
