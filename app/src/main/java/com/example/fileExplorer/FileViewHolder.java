package com.example.fileExplorer;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class FileViewHolder extends RecyclerView.ViewHolder {
    public TextView tvName, tvSize;
    public CardView container;
    public ImageView imgFile;
    public FrameLayout iconBg;

    public FileViewHolder(@NonNull View itemView) {
        super(itemView);
        tvName    = itemView.findViewById(R.id.tv_fileName);
        tvSize    = itemView.findViewById(R.id.tvFileSize);
        container = itemView.findViewById(R.id.container);
        imgFile   = itemView.findViewById(R.id.img_fileType);
        iconBg    = itemView.findViewById(R.id.icon_bg);
    }
}
