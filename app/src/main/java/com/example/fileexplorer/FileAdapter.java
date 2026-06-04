package com.example.fileexplorer;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileViewHolder> {

    private Context context;
    private List<File> file;

    public FileAdapter(Context context, List<File> file) {
        this.context = context;
        this.file = file;
    }


    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FileViewHolder(LayoutInflater.from(context).inflate(R.layout.file_container, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {

        holder.tvName.setText(file.get(position).getName());
        holder.tvName.setSelected(true);
        int items = 0;

        if (file.get(position).isDirectory()) {
            File[] files = file.get(position).listFiles();
            if (files != null) {
                for (File singleFile : files) {
                    if (!singleFile.isHidden()) {
                        items += 1;
                    }
                }
            }
            holder.tvSize.setText(String.valueOf(items) + " Files");
        } else {
            holder.tvSize.setText(Formatter.formatShortFileSize(context, file.get(position).length()));
        }


        String name = file.get(position).getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")) {
            holder.imgFile.setImageResource(R.drawable.ic_image);
        } else if (name.endsWith(".pdf")) {
            holder.imgFile.setImageResource(R.drawable.ic_pdf);
        } else if (name.endsWith(".mp3") || name.endsWith(".wav")) {
            holder.imgFile.setImageResource(R.drawable.ic_music);
        } else if (name.endsWith(".apk")) {
            holder.imgFile.setImageResource(R.drawable.ic_android);
        } else if (name.endsWith(".mp4")) {
            holder.imgFile.setImageResource(R.drawable.ic_play);
        } else if (name.endsWith(".doc")) {
            holder.imgFile.setImageResource(R.drawable.ic_docs);
        } else {
            holder.imgFile.setImageResource(R.drawable.ic_folder);
        }
    }

    @Override
    public int getItemCount() {
        return file.size();
    }
}
