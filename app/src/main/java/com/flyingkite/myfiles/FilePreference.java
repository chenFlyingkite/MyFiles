package com.flyingkite.myfiles;

import java.io.File;
import java.util.Comparator;
import java.util.Map;

import flyingkite.library.android.log.Loggable;
import flyingkite.library.android.preference.EasyPreference;
import flyingkite.library.java.util.MathUtil;

public class FilePreference extends EasyPreference {
    public FilePreference() {
        super(App.me, "FilePreference");
    }

    public NextPref fileListSort = new NextPref("fileListSort", fileListSortBy.length);

    public class NextPref extends IntPref {
        private int count;
        public NextPref(String _key, int _count) {
            super(_key);
            count = _count;
        }

        public NextPref(String _key, int defValue, int _count) {
            super(_key, defValue);
            count = _count;
        }

        public int next() {
            return move(1);
        }

        public int prev() {
            return move(count - 1);
        }

        public int move(int x) {
            int v = get();
            int next = (v + x) % count;
            set(next);
            return next;
        }
    }

    public static final int[] fileListSortBy = {
            R.string.fileList_Name_A_to_Z,
            R.string.fileList_Name_Z_to_A,
            R.string.fileList_Size_small_to_large,
            R.string.fileList_Size_large_to_small,
            R.string.fileList_Date_old_to_new,
            R.string.fileList_Date_new_to_old,
    };

    private static Loggable z = new Loggable() {
    };

    public static Comparator<File> getComparatorFileList(Map<File, ?> info) {
        FilePreference pref = new FilePreference();
        int k = pref.fileListSort.get();
        k = MathUtil.makeInRange(k, 0, fileListSortBy.length);
        int sort = fileListSortBy[k];
        if (sort == R.string.fileList_Date_old_to_new || sort == R.string.fileList_Date_new_to_old) {
            return new Comparator<>() {
                private long base(File f) {
                    return f == null ? 0 : f.lastModified();
                }
                @Override
                public int compare(File x, File y) {
                    boolean asc = sort == R.string.fileList_Date_old_to_new;
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
                        Object o = info.get(f);
                        if (o instanceof Long) {
                            return (long) o;
                        }
                    }
                    return f == null ? 0 : f.length();
                }
                @Override
                public int compare(File x, File y) {
                    boolean asc = sort == R.string.fileList_Size_small_to_large;
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
            String xn = x == null ? "" : x.getName();
            String yn = y == null ? "" : y.getName();
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
