package com.example.fileexplorer;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileViewHolder> {

    private final Context context;
    private final List<File> file;
    private final OnFileSelectedListener listener;

    public FileAdapter(Context context, List<File> file, OnFileSelectedListener listener) {
        this.context  = context;
        this.file     = file;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileViewHolder(
                LayoutInflater.from(context).inflate(R.layout.file_container, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File current = file.get(position);

        // --- Name ---
        holder.tvName.setText(current.getName());

        // --- Size / item count ---
        if (current.isDirectory()) {
            File[] children = current.listFiles();
            int count = 0;
            if (children != null) {
                for (File f : children) {
                    if (!f.isHidden()) count++;
                }
            }
            holder.tvSize.setText(count + " files");
        } else {
            holder.tvSize.setText(Formatter.formatShortFileSize(context, current.length()));
        }

        // --- Icon + accent color ---
        String name = current.getName().toLowerCase();
        int iconRes;
        int accentColor;

        if (current.isDirectory()) {
            iconRes     = R.drawable.ic_folder;
            accentColor = ContextCompat.getColor(context, R.color.color_folder);
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".raw") || name.endsWith(".dng")) {
            iconRes     = R.drawable.ic_image;
            accentColor = ContextCompat.getColor(context, R.color.color_image);
        } else if (name.endsWith(".pdf")) {
            iconRes     = R.drawable.ic_pdf;
            accentColor = ContextCompat.getColor(context, R.color.color_pdf);
        } else if (name.endsWith(".mp3") || name.endsWith(".wav")) {
            iconRes     = R.drawable.ic_music;
            accentColor = ContextCompat.getColor(context, R.color.color_audio);
        } else if (name.endsWith(".mp4")) {
            iconRes     = R.drawable.ic_play;
            accentColor = ContextCompat.getColor(context, R.color.color_video);
        } else if (name.endsWith(".doc") || name.endsWith(".docx")) {
            iconRes     = R.drawable.ic_docs;
            accentColor = ContextCompat.getColor(context, R.color.color_doc);
        } else if (name.endsWith(".txt")) {
            iconRes     = R.drawable.ic_docs;
            accentColor = ContextCompat.getColor(context, R.color.color_txt);
        } else if (name.endsWith(".apk")) {
            iconRes     = R.drawable.ic_android;
            accentColor = ContextCompat.getColor(context, R.color.color_apk);
        } else {
            iconRes     = R.drawable.ic_folder;
            accentColor = ContextCompat.getColor(context, R.color.color_generic);
        }

        holder.imgFile.setImageResource(iconRes);
        // Tint the icon with the accent color
        holder.imgFile.setColorFilter(accentColor);

        // Color the circular icon background with a semi-transparent version of the accent
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        int bgColor = (accentColor & 0x00FFFFFF) | 0x33000000; // ~20% alpha
        circle.setColor(bgColor);
        holder.iconBg.setBackground(circle);

        // --- Click listeners ---
        holder.container.setOnClickListener(v -> listener.onFileClick(current));
        holder.container.setOnLongClickListener(v -> {
            listener.onFileLongClick(current);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return file.size();
    }
}
