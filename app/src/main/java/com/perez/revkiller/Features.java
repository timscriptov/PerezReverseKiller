package com.perez.revkiller;

public class Features {
    public static native int ExtractAllRAR(String f, String d);

    public static native boolean Oat2Dex(String f);

    public static native boolean Odex2Dex(String file, String dest);

    public static native boolean ZipAlign(String zip, String destZip);

    public static native boolean isZipAligned(String zip);

    public static native boolean isValidElf(String elf);

    public static native String compressStrToInt(String str);

    public static native long ELFHash(String strUri);

    public static native int dumpDex(int apiLevel, String appEntry);

    public native static String AStyleMain(String text, String opt);

}
