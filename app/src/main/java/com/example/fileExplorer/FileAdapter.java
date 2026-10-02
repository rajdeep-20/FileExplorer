package com.example.fileExplorer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileViewHolder> {

    private final Context context;
    private final List<FileItem> items;
    private final OnFileSelectedListener listener;
    private final ThumbnailLoader thumbnailLoader;

    public FileAdapter(Context context, List<FileItem> items, OnFileSelectedListener listener) {
        this.context  = context;
        this.items    = items;
        this.listener = listener;
        this.thumbnailLoader = new ThumbnailLoader(context);
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

        // --- Size text ---
        if (!current.isMetadataLoaded()) {
            holder.tvSize.setText("...");
        } else if (current.isDirectory()) {
            holder.tvSize.setText(R.string.folder);
        } else {
            holder.tvSize.setText(Formatter.formatShortFileSize(context, current.getFileSize()));
        }

        // --- Icon & color via FileTypeRegistry ---
        FileTypeRegistry type = (current.isMetadataLoaded() && current.isDirectory())
                ? FileTypeRegistry.FOLDER
                : FileTypeRegistry.fromFileName(current.getName());

        int iconRes = type.getIconRes();
        int accentColor = type.getAccentColor(context);

        holder.imgFile.setImageResource(iconRes);
        holder.imgFile.setColorFilter(accentColor);

        // --- Async thumbnail loading ---
        thumbnailLoader.loadThumbnail(holder.imgFile, current, type, iconRes, accentColor);

        // --- Icon background circle ---
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor((accentColor & 0x00FFFFFF) | 0x33000000);
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
}
