package com.flyingkite.myfiles.util;

import android.util.Pair;

import com.flyingkite.myfiles.App;
import com.flyingkite.myfiles.R;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.preference.EasyPreference;
import flyingkite.library.java.data.FileInfo;

public class FilePreference extends EasyPreference {
    public FilePreference() {
        super(App.me, "FilePreference");
    }

    public StringPref fileSort = new StringPref("fileSort", App.me.getString(R.string.nameAZ));

    public static final List<Pair<Integer, String>> fileSortBy = Arrays.asList(
            of(R.string.fileList_Name_A_to_Z, R.string.nameAZ),
            of(R.string.fileList_Name_Z_to_A, R.string.nameZA),
            of(R.string.fileList_Size_small_to_large, R.string.size01),
            of(R.string.fileList_Size_large_to_small, R.string.size10),
            of(R.string.fileList_Date_old_to_new, R.string.date01),
            of(R.string.fileList_Date_new_to_old, R.string.date10)
    );

    private static Pair<Integer, String> of(int id, int keyID) {
        return new Pair<>(id, App.me.getString(keyID));
    }

    private static final Loggable z = new Loggable() {
    };

    private static int findSort(String key) {
        for (int i = 0; i < fileSortBy.size(); i++) {
            if (fileSortBy.get(i).second.equals(key)) {
                return i;
            }
        }
        return 0;
    }

    public static String sortString() {
        FilePreference pref = new FilePreference();
        int at = findSort(pref.fileSort.get());
        return App.me.getString(fileSortBy.get(at).first);
    }

    // Return compare result for Folder < File, that is
    // Return  1, If x is file, y is directory (x > y)
    // Return -1, If x is directory, y is file (x < y)
    // Return 0 for both x and y are directory or file
    private static int compareFolderFile(File x, File y) {
        boolean fx = x.isDirectory();
        boolean fy = y.isDirectory();
        if (fx != fy) {
            return fx ? -1 : 1;
        }
        return 0;
    }

    public static Comparator<File> getComparatorFileList(Map<File, FileInfo> info) {
        FilePreference pref = new FilePreference();
        String key = pref.fileSort.get();
        int k = findSort(key);
        int sort = fileSortBy.get(k).first;
        if (sort == R.string.fileList_Date_old_to_new || sort == R.string.fileList_Date_new_to_old) {
            return new Comparator<>() {
                private long base(File f) {
                    if (info != null) {
                        FileInfo fi = info.get(f);
                        if (fi != null) {
                            return fi.lastModified;
                        }
                    }
                    return f == null ? 0 : f.lastModified();
                }
                @Override
                public int compare(File x, File y) {
                    boolean asc = sort == R.string.fileList_Date_old_to_new;
                    int type = compareFolderFile(x, y);
                    if (type != 0) {
                        return type;
                    }
                    long xn = base(x);
                    long yn = base(y);
                    if (asc) {
                        // R.string.fileList_Date_old_to_new
                        return Long.compare(xn, yn);
                    } else {
                        // R.string.fileList_Date_new_to_old
                        return Long.compare(yn, xn);
                    }
                }
            };
        } else if (sort == R.string.fileList_Size_small_to_large || sort == R.string.fileList_Size_large_to_small) {
            return new Comparator<>() {
                private long base(File f) {
                    // get from info if it is Map<File, Long>
                    if (info != null) {
                        FileInfo fi = info.get(f);
                        if (fi != null) {
                            return fi.fileSize;
                        }
                    }
                    return f == null ? 0 : f.length();
                }
                @Override
                public int compare(File x, File y) {
                    boolean asc = sort == R.string.fileList_Size_small_to_large;
                    int type = compareFolderFile(x, y);
                    if (type != 0) {
                        return type;
                    }
                    long xn = base(x);
                    long yn = base(y);
                    if (asc) {
                        // R.string.fileList_Size_small_to_large
                        return Long.compare(xn, yn);
                    } else {
                        // R.string.fileList_Size_large_to_small
                        return Long.compare(yn, xn);
                    }
                }
            };
        }

        return (x, y) -> {
            boolean asc = sort == R.string.fileList_Name_A_to_Z;
            int type = compareFolderFile(x, y);
            if (type != 0) {
                return type;
            }
            String xn = x.getName();
            String yn = y.getName();
            if (asc) {
                // R.string.fileList_Name_A_to_Z,
                return xn.compareTo(yn);
            } else {
                // R.string.fileList_Name_Z_to_A
                return yn.compareTo(xn);
            }
        };
    }



}
