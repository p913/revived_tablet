package ru.revivedtablet.config;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import androidx.core.content.ContextCompat;


import ru.revivedtablet.ImageUtils;
import ru.revivedtablet.RevivedTabletApp;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class Storages {

    private static final Storages instance = new Storages();

    private List<Storage> storages = new ArrayList<Storage>();

    private Storages() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            obtainStoragesBeforeKitKat();
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            obtainStoragesKitKat();

        storages.add(new Storage("Внутренняя память устройства",
                RevivedTabletApp.getContext().getDir(Environment.DIRECTORY_PICTURES, Context.MODE_PRIVATE)));
    }

    private void obtainStoragesKitKat() {
        File[] ff = ContextCompat.getExternalFilesDirs(RevivedTabletApp.getContext(), Environment.DIRECTORY_PICTURES);
        for (File f: ff)
            if (f != null)
                storages.add(new ExternalStorage("Внешняя память устройства", f));
    }

    /**
     * Based on
     * https://stackoverflow.com/questions/11281010/how-can-i-get-external-sd-card-path-for-android-4-0/13648873#13648873
     */
    private void obtainStoragesBeforeKitKat() {
        final HashSet<String> out = new HashSet<String>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        String s = "";
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                s = s + new String(buffer);
            }
            is.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // parse output
        final String[] lines = s.split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec")) {
                if (line.matches(reg)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/"))
                            if (!part.toLowerCase(Locale.US).contains("vold"))
                                out.add(part);
                    }
                }
            }
        }

        for (String ss: out)
            storages.add(new ExternalStorage("Внешняя память устройства", new File(ss, Environment.DIRECTORY_PICTURES) ));
    }

    public Storage findStorage(File path) {
        if (path != null) {
            for (Storage s : getAvailable()) {
                if (path.getAbsolutePath().indexOf(s.getPath()) == 0)
                    return s;
            }
        }
        return null;
    }

    public static Storages getInstance() {
        return instance;
    }

    public List<Storage> getAvailable() {
        if (isAllAvailable())
            return storages;
        else {
            List<Storage> res = new ArrayList<>();
            for (Storage s: storages)
                if (s.isAvailable())
                    res.add(s);
            return res;
        }
    }

    private boolean isAllAvailable() {
        for (Storage s: storages)
            if (!s.isAvailable())
                return false;
        return true;
    }

    public static class Storage {
        protected String name;
        protected String usage;
        protected File dir;

        public Storage(String name, File dir) {
            this.name = name;
            this.usage = "";
            this.dir = dir;

            calcUsages();

            Log.d("Storage " + name, dir == null ? "null" : dir.getAbsolutePath());
        }

        protected boolean isAvailable() {
            return true;
        }

        protected void calcUsages() {
            try {
                StatFs s = new StatFs(dir.getAbsolutePath());
                usage = formatSize((long) s.getFreeBlocks() * s.getBlockSize()) + " свободно из " +
                        formatSize((long) s.getBlockCount() * s.getBlockSize());
            } catch (Exception e) {
            }
        }

        public String getName() {
            return name;
        }

        public String getUsage() {
            return usage;
        }

        public void updateUsages() {
            calcUsages();
        }

        public String getPath() {
            return dir.getAbsolutePath();
        }

        public String getId() {
            return String.valueOf(getName().hashCode());
        }

        public Folder[] getFolders() {
            File[] dirs = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
            if (dirs != null) {
                Folder[] res = new Folder[dirs.length + 1];
                res[0] = new Folder(dir, ".");
                for (int i = 0; i < dirs.length; i++)
                    res[i + 1] = new Folder(dirs[i]);
                return res;
            } else
                return new Folder[] {new Folder(dir, ".")};
        }

        public static String formatSize(long sizeInBytes) {
            long devider = 1024 * 1024 * 1024;
            if (sizeInBytes > devider)
                return String.format("%1.2f ГБ", (double)sizeInBytes / devider);

            devider /= 1024;
            if (sizeInBytes > devider)
                return String.format("%1.2f МБ", (double)sizeInBytes / devider);

            devider /= 1024;
            if (sizeInBytes > devider)
                return String.format("%1.2f КБ", (double)sizeInBytes / devider);

            return String.format("%d Б", sizeInBytes);

        }
    }

    /**
     * Внешнее хранилище, например, SD-Card, м.б. недоступно
     */
    public class ExternalStorage extends Storage {

        public ExternalStorage (String name, File dir) {
            super(name, dir);
        }

        @Override
        protected boolean isAvailable() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                return Environment.getStorageState(dir).equals(Environment.MEDIA_MOUNTED);
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                return Environment.getExternalStorageState(dir).equals(Environment.MEDIA_MOUNTED);
            else
                return dir.exists();
        }

    }

    public static class Folder {
        private File dir;
        private String name;

        public Folder(File dir) {
            this.dir = dir;
            this.name = dir.getName();
        }

        public Folder(File dir, String name) {
            this.dir = dir;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return dir.getAbsolutePath();
        }

        public String getId() {
            return String.valueOf(getName().hashCode());
        }

        public File[] getPictureFiles() {
            File [] f =  dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(ImageUtils.PICTURE_EXTENSION);
                }
            });
            return f == null ? new File[] {} : f;
        }

        public Picture[] getPictures() {
            File[] pics = getPictureFiles();
            Picture[] res = new Picture[pics.length];
            for (int i = 0; i < pics.length; i++)
                res[i] = new Picture(pics[i]);
            return res;
        }

        public String getDescription() {
            File[] files = getPictureFiles();
            long size = 0;
            for (File f: files)
                size += f.length();
            return String.format("%d изображений(я), %s", files.length, Storage.formatSize(size));
        }
    }

    public static class Picture {
        private File file;

        public Picture(File file) {
            this.file = file;
        }

        public String getName() {
            return file.getName().replace(ImageUtils.PICTURE_EXTENSION, "");
        }

        public String getPath() {
            return file.getAbsolutePath();
        }

        public String getSize() {
            return Storage.formatSize(file.length());
        }
    }
}
