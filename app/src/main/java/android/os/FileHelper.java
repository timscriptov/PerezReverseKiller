package android.os;

public class FileHelper {

    public static final class PassWord {
        public String name;
        public String passwd;
        public int uid;
        public int gid;
        public String gecos;
        public String dir;
        public String shell;
    }

    public static final class Group {
        public String name;
        public String passwd;
        public int gid;
        public String[] mem;
    }

    public static final class FileStatus {
        public int dev;
        public int ino;
        public int mode;
        public int nlink;
        public int uid;
        public int gid;
        public int rdev;
        public long size;
        public int blksize;
        public long blocks;
        public long atime;
        public long mtime;
        public long ctime;
    }

    public static native boolean stat(String path, FileStatus status);

    public static native int setPermissions(String file, int mode, int uid, int gid);

    public static native int getPermissions(String file, int[] outPermissions);

    public static native PassWord getpwuid(int uid);

    public static native int chown(String file, int uid, int gid);

    public static native int chmod(String file, int mode);

    public static native Group getgrgid(int gid);

    public static native int getFatVolumeId(String mountPoint);

    static {

        try {
            System.loadLibrary("FileHelper");
        } catch(UnsatisfiedLinkError ule) {
            System.err.println("WARNING: Could not load library : FileHelper!");
        }
    }

}
