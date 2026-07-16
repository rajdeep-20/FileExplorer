package com.example.fileexplorer.fragments;

import com.example.fileexplorer.FileItem;
import java.util.Comparator;

public class SortingOrder {
    public enum SortingOrderEnum {
        NAME_ASC(Comparator.comparing(item -> item.getName().toLowerCase())),
        NAME_DESC(Comparator.comparing((FileItem item) -> item.getName().toLowerCase()).reversed()),
        TIME_ASC(Comparator.comparingLong(FileItem::getLastModified)),
        TIME_DESC(Comparator.comparingLong(FileItem::getLastModified).reversed()),
        SIZE_ASC(Comparator.comparingLong(FileItem::getFileSize)),
        SIZE_DESC(Comparator.comparingLong(FileItem::getFileSize).reversed());

        private final Comparator<FileItem> comparator;

        SortingOrderEnum(Comparator<FileItem> comparator) {
            this.comparator = comparator;
        }

        public Comparator<FileItem> getComparator() {
            return this.comparator;
        }
    }
}
