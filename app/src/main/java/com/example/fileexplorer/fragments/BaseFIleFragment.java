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

public abstract class BaseFIleFragment extends Fragment implements OnFileSelectedListener {
    protected RecyclerView recyclerView;
    protected List<File> fileList = new ArrayList<>();
    protected FileAdapter fileAdapter;
    protected List<File> MasterFile = new ArrayList<>();

    protected View view;
    protected String[] items = {"Details", "Rename", "Delete", "Share"};

    private int currentPage = 0;
    private int chunkPage = 100;
    private boolean isLoading = false;

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

    protected abstract List<File> getFilesToDisplay();

    protected abstract int getRecyclerView();

    protected abstract void openDirectory(File file);

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
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 5));
        fileAdapter = new FileAdapter(getContext(), fileList, this);
        recyclerView.setAdapter(fileAdapter);

        setupScrollListener();

        new Thread(() -> {
            List<File> result = getFilesToDisplay();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    MasterFile = new ArrayList<>(result);
                    fileList.clear();
                    fileAdapter.notifyDataSetChanged();
                    currentPage = 0;
                    LoadPages();
                });
            }
        }).start();
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && !isLoading && !recyclerView.isComputingLayout() && layoutManager.findLastCompletelyVisibleItemPosition() >= fileList.size() - 5) {
                        LoadPages();
                    }
                }
            }
        });
    }

    private void LoadPages() {
        if (MasterFile.isEmpty())
            return;
        int start = currentPage * chunkPage;
        if (start >= MasterFile.size())
            return;

        isLoading = true;
        int end = Math.min(start + chunkPage, MasterFile.size());

        List<File> newItems = MasterFile.subList(start, end);

        int insertPos = fileList.size();
        fileList.addAll(newItems);
        fileAdapter.notifyItemRangeInserted(insertPos, newItems.size());

        currentPage += 1;
        isLoading = false;
    }

    @Override
    public void onFileClick(File file) {
        if (file.isDirectory()) {
            openDirectory(file);
        } else {
            try {
                FileOpener.openFile(getContext(), file);
            } catch (IOException e) {
                Toast.makeText(getContext(), "Error opening File", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("SetText18n")
    @Override
    public void onFileLongClick(File file, int position) {
        final Dialog optionalDialog = new Dialog(getContext());
        optionalDialog.setContentView(R.layout.option_diallogue);
        ListView listView = optionalDialog.findViewById(R.id.List);
        listView.setAdapter(new CustomAdpator());
        listView.setOnItemClickListener((parent, v, pos, id) -> {
            handleOptionClick(items[pos], file, optionalDialog);
        });
        optionalDialog.show();
    }

    private void handleOptionClick(String selectedItem, File file, Dialog dialog) {
        if (selectedItem.equals("Details")) {
            showDetails(file);
        } else if (selectedItem.equals("Rename")) {
            showRenameDailog(file);
        } else if (selectedItem.equals("Share")) {
            shareFile(file);
        } else if (selectedItem.equals("Delete")) {
            showDeletedDialog(file);
        }
        dialog.dismiss();
    }

    private void showDeletedDialog(File file) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete")
                .setMessage("Delete " + file.getName() + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (file.delete()) {
                        int currentPos = fileList.indexOf(file);
                        if (currentPos != -1) {
                            fileList.remove(currentPos);
                            fileAdapter.notifyItemRemoved(currentPos);
                            MasterFile.remove(file);
                            Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showDetails(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Details");
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(file.lastModified()));
        builder.setMessage("Name: " + file.getName() + "\nSize: " + Formatter.formatShortFileSize(getContext(), file.length()) + "\nPath: " + file.getAbsolutePath() + "\nModified: " + date);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showRenameDailog(File file) {
        AlertDialog.Builder rename = new AlertDialog.Builder(getContext());
        rename.setTitle("Rename");
        EditText input = new EditText(getContext());
        input.setText(file.getName());
        rename.setView(input);
        rename.setPositiveButton("OK", (d, w) -> {
            String newName = input.getText().toString();
            File dest = new File(file.getParent(), newName);

            if (file.renameTo(dest)) {
                int currentPos = fileList.indexOf(file);
                if (currentPos != -1) {
                    fileList.set(currentPos, dest);
                    fileAdapter.notifyItemChanged(currentPos);
                    int masterPos = MasterFile.indexOf(file);
                    if (masterPos != -1) {
                        MasterFile.set(masterPos, dest);
                    }
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
