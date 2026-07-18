package com.example.fileExplorer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileViewHolder> {

    private final Context context;
    private final List<FileItem> items;
    private final OnFileSelectedListener listener;

    public FileAdapter(Context context, List<FileItem> items, OnFileSelectedListener listener) {
        this.context  = context;
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileViewHolder(
                LayoutInflater.from(context).inflate(R.layout.file_container, parent, false));
    }

    @SuppressLint("SetText18n")
    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem current = items.get(position);

        holder.tvName.setText(current.getName());

        if (!current.isMetadataLoaded()) {
            holder.tvSize.setText("..."); // Loading metadata
        } else if (current.isDirectory()) {
            holder.tvSize.setText(R.string.folder);
        } else {
            holder.tvSize.setText(Formatter.formatShortFileSize(context, current.getFileSize()));
        }

        String name = current.getName().toLowerCase();
        int iconRes;
        int accentColor;
        boolean isMedia = false;

        // Note: Default to generic until metadata is loaded if you prefer,
        // but here we use extension as a hint even before metadata.
        if (current.isMetadataLoaded() && current.isDirectory()) {
            iconRes     = R.drawable.ic_folder;
            accentColor = ContextCompat.getColor(context, R.color.color_folder);
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".raw") || name.endsWith(".dng") || name.endsWith(".webp")) {
            iconRes     = R.drawable.ic_image;
            accentColor = ContextCompat.getColor(context, R.color.color_image);
            isMedia = true;
        } else if (name.endsWith(".pdf")) {
            iconRes     = R.drawable.ic_pdf;
            accentColor = ContextCompat.getColor(context, R.color.color_pdf);
        } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a")) {
            iconRes     = R.drawable.ic_music;
            accentColor = ContextCompat.getColor(context, R.color.color_audio);
        } else if (name.endsWith(".mp4") || name.endsWith(".mkv")) {
            iconRes     = R.drawable.ic_play;
            accentColor = ContextCompat.getColor(context, R.color.color_video);
            isMedia = true;
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

        // --- Skeleton/Generic Loading ---
        holder.imgFile.setImageResource(iconRes);
        holder.imgFile.setColorFilter(accentColor);

        // --- Asynchronous Thumbnail Loading with Glide ---
        if (isMedia) {
            Glide.with(context)
                    .load(new File(current.getAbsolutePath()))
                    .placeholder(iconRes)
                    .centerCrop()
                    .into(holder.imgFile);
            holder.imgFile.clearColorFilter();
        } else {
            Glide.with(context).clear(holder.imgFile);
        }

        // Color the circular icon background
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        int bgColor = (accentColor & 0x00FFFFFF) | 0x33000000;
        circle.setColor(bgColor);
        holder.iconBg.setBackground(circle);

        // --- Click listeners ---
        holder.container.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onFileItemClick(items.get(pos));
            }
        });
        holder.container.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onFileItemLongClick(items.get(pos), pos);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    public void submitList(List<FileItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }
}
