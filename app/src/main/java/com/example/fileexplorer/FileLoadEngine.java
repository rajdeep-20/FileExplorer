package com.example.fileexplorer;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * The Android 14 Directory Engine
 * This service loads the directory structure instantly via streaming NIO paths on a background thread,
 * updates the layout list, and immediately kicks off metadata scanning without blocking the user.
 */
public class FileLoadEngine {

    // Dedicated thread pools to isolate I/O tasks
    private final ExecutorService structureExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService metadataExecutor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface FileLoadListener {
        void onStructureLoaded(List<FileItem> items);
        void onItemsAdded(List<FileItem> newItems);
        void onItemMetadataUpdated(int position, FileItem updatedItem);
    }

    public void loadDirectory(String targetDirectoryPath, FileLoadListener listener) {
        structureExecutor.execute(() -> {
            List<FileItem> fileList = new ArrayList<>();
            Path dir = Paths.get(targetDirectoryPath);

            // Phase 1: Instant Stream Scan
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString();
                    String fullPath = entry.toAbsolutePath().toString();
                    FileItem item = new FileItem(name, fullPath);
                    
                    // Quick check for directory (usually cached in stream)
                    if (Files.isDirectory(entry)) {
                        item.updateMetadata(true, 0, 0);
                    }
                    
                    fileList.add(item);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Publish structural list immediately to fix scrollbar positioning
            mainHandler.post(() -> {
                listener.onStructureLoaded(fileList);
                // Phase 2: Trigger lazy background metadata population
                fetchMetadataAsynchronously(fileList, listener);
            });
        });
    }

    public void loadRecursive(String targetDirectoryPath, Predicate<Path> filter, FileLoadListener listener) {
        structureExecutor.execute(() -> {
            Path startPath = Paths.get(targetDirectoryPath);
            List<FileItem> batch = new ArrayList<>();
            final int BATCH_SIZE = 50;

            try {
                Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (dir.getFileName() != null && dir.getFileName().toString().startsWith(".")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (filter.test(file)) {
                            FileItem item = new FileItem(file.getFileName().toString(), file.toAbsolutePath().toString());
                            item.updateMetadata(attrs.isDirectory(), attrs.size(), attrs.lastModifiedTime().toMillis());
                            batch.add(item);

                            if (batch.size() >= BATCH_SIZE) {
                                List<FileItem> currentBatch = new ArrayList<>(batch);
                                batch.clear();
                                mainHandler.post(() -> listener.onItemsAdded(currentBatch));
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });

                if (!batch.isEmpty()) {
                    mainHandler.post(() -> listener.onItemsAdded(batch));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void fetchMetadataAsynchronously(List<FileItem> fileList, FileLoadListener listener) {
        for (int i = 0; i < fileList.size(); i++) {
            final int index = i;
            final FileItem item = fileList.get(index);

            metadataExecutor.execute(() -> {
                try {
                    Path path = Paths.get(item.getAbsolutePath());
                    
                    // Android 14 C-level Optimization: Grabs ALL attributes in 1 atomic file system request
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    
                    item.updateMetadata(attrs.isDirectory(), attrs.size(), attrs.lastModifiedTime().toMillis());

                    // Inform the adapter to redraw only this item
                    mainHandler.post(() -> listener.onItemMetadataUpdated(index, item));

                } catch (IOException e) {
                    // Handle broken symlinks or permission changes gracefully
                    e.printStackTrace();
                }
            });
        }
    }
}
