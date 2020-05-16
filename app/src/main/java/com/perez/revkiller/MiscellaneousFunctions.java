package com.perez.revkiller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.googlecode.d2j.dex.Dex2jar;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MiscellaneousFunctions {

    public static boolean chkperm(Context ctx, String permid) {
        if(ContextCompat.checkSelfPermission(ctx, permid) != PackageManager.PERMISSION_GRANTED) return false;
        else return true;
    }

    public static void DecompileJAR(String zipfile) {
        String strpath = (new File(zipfile)).getParent()+"/decompile";
        File fn = new File(strpath);
        if(!fn.exists()) fn.mkdir();
        ConsoleDecompiler.DoDecompile(zipfile, strpath);
    }

    public static boolean isStandardJAR(String zip) {
        boolean value = false;
        try {
            ZipFile zipFile = new ZipFile(zip);
            Enumeration<ZipEntry> enu = (Enumeration<ZipEntry>) zipFile.entries();
            while(enu.hasMoreElements()) {
                ZipEntry zipElement = (ZipEntry) enu.nextElement();
                zipFile.getInputStream(zipElement);
                String fileName = zipElement.getName();
                if(fileName.endsWith(".class")) {
                    value = true;
                    break;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private static boolean reuseReg = true;
    private static boolean debugInfo = false;
    private static boolean printIR = false;
    private static boolean optmizeSynchronized = true;
    private static boolean skipExceptions = true;
    private static boolean noCode = false;

    private static byte[] getBytes(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while((n = fis.read(b)) != -1)
                bos.write(b, 0, n);
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static void DexTrans(File in, File out) throws IOException {
        long baseTS = System.currentTimeMillis();
        byte [] dexb = getBytes(in.getAbsolutePath());
        Dex2jar.from(dexb)
                .reUseReg(reuseReg)
                .topoLogicalSort()
                .skipDebug(!debugInfo)
                .optimizeSynchronized(optmizeSynchronized)
                .printIR(printIR)
                .noCode(noCode)
                .skipExceptions(skipExceptions)
                .to(out);
        long endTS = System.currentTimeMillis();
        System.out.println(String.format("Dex2Jar Consume Time->%.2f", (float)(endTS - baseTS) / 1000));
    }
    public static void DexTrans(String in, String out) throws IOException {
        DexTrans(new File(in), new File(out));
    }

    public static String readCodeFromFile(Context ctx,String filePath) {

        File inFile = new File(filePath);
        final int readSize =  131072;
        StringBuffer bufferIn = new StringBuffer(readSize);
        char fileIn[] = new char[readSize];

        try {
            BufferedReader in =
                    new BufferedReader(new FileReader(inFile));

            int charsIn = in.read(fileIn, 0, readSize);
            while(charsIn != -1) {
                bufferIn.append(fileIn, 0, charsIn);
                charsIn = in.read(fileIn, 0, readSize);
            }
            in.close();
        } catch(Exception e) {
            if(e instanceof FileNotFoundException) {
                Toast.makeText(ctx, "Cannot open input file " + filePath, Toast.LENGTH_LONG).show();
                return "";
            } else if(e instanceof IOException) {
                Toast.makeText(ctx, "Error reading file " + filePath, Toast.LENGTH_LONG).show();
                return "";
            } else {
                Toast.makeText(ctx, e.getMessage() + " " + filePath, Toast.LENGTH_LONG).show();
                return "";
            }
        }
        return bufferIn.toString();
    }

     public static boolean writeCodeToFile(Context ctx,String textOut, String filePath) {

        String origfilePath = filePath +  ".orig";
        File origFile = new File(origfilePath);
        File outFile = new File(filePath);
        origFile.delete();
        if(!outFile.renameTo(origFile)) {
            Toast.makeText(ctx, "Cannot create backup file " + origfilePath, Toast.LENGTH_LONG).show();
            return false;
        }

        try {
            BufferedWriter out =
                    new BufferedWriter(new FileWriter(filePath));
            out.write(textOut, 0, textOut.length());
            out.close();
        } catch(IOException e) {
            Toast.makeText(ctx, "Cannot write to output " + filePath, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public static boolean formatting_code(Context ctx,String fn_path) {
        String textIn = readCodeFromFile(ctx,fn_path);
        String options = "-style=java";
        String textOut = Features.AStyleMain(textIn, options);
        if(textIn.isEmpty()) return false;
        if(!writeCodeToFile(ctx,textOut, fn_path)) return false;
        return true;
    }

    public static String convertSeconds(long tm) {
        long hour,minute,second;
        second = tm;
        hour = second/3600;
        minute = (second - hour*3600)/60;
        second = second-hour*300-minute*60;
        String str = String.format("%l:%.2l:%.2l",hour,minute,second);
        return str;
    }

    public static String convertBytesLength(long size) {
        DecimalFormat formater = new DecimalFormat("####.00");
        if(size < 1024)
            return size + "B";
        else if(size < 1024 * 1024) {
            float kbsize = size / 1024f;
            return formater.format(kbsize) + "KB";
        } else if(size < 1024 * 1024 * 1024) {
            float mbsize = size / 1024f / 1024f;
            return formater.format(mbsize) + "MB";
        } else {
            float gbsize = size / 1024f / 1024f / 1024f;
            return formater.format(gbsize) + "GB";
        }
    }

    public static boolean isZip(File file) {
        String name = file.getName().toLowerCase();
        if(name.endsWith(".apk"))
            return true;
        if(name.endsWith(".zip") || name.endsWith(".jar"))
            return true;
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void startInstallPermissionSettingActivity(Activity ctx) {
        Uri packageURI = Uri.parse("package:" + ctx.getPackageName());

        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        ctx.startActivityForResult(intent, 10086);
    }

    public static void installApk(Activity ctx,File apk,File mCurrent) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
        } else {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getApplicationContext().getPackageName() + ".provider", mCurrent);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.getBaseContext().startActivity(intent);
    }

    private static final String TEMP_DIR = "system_config";
    private static final String TEMP_FILE_NAME = "system_file";
    private static final String TEMP_FILE_NAME_MIME_TYPE = "application/octet-stream";
    private static final String SP_NAME = "device_info";
    private static final String SP_KEY_DEVICE_ID = "device_id";

    public static String getDeviceId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String deviceId = sharedPreferences.getString(SP_KEY_DEVICE_ID, null);
        if (!TextUtils.isEmpty(deviceId)) {
            return deviceId;
        }
        deviceId = getIMEI(context);
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = createUUID(context);
        }
        sharedPreferences.edit()
                .putString(SP_KEY_DEVICE_ID, deviceId)
                .apply();
        return deviceId;
    }

    public static String createUUID(Context context) {
        String uuid = UUID.randomUUID().toString().replace("-", "");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Uri externalContentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            ContentResolver contentResolver = context.getContentResolver();
            String[] projection = new String[]{
                    MediaStore.Downloads._ID
            };
            String selection = MediaStore.Downloads.TITLE + "=?";
            String[] args = new String[]{
                    TEMP_FILE_NAME
            };
            Cursor query = contentResolver.query(externalContentUri, projection, selection, args, null);
            if (query != null && query.moveToFirst()) {
                Uri uri = ContentUris.withAppendedId(externalContentUri, query.getLong(0));
                query.close();

                InputStream inputStream = null;
                BufferedReader bufferedReader = null;
                try {
                    inputStream = contentResolver.openInputStream(uri);
                    if (inputStream != null) {
                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        uuid = bufferedReader.readLine();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Downloads.TITLE, TEMP_FILE_NAME);
                contentValues.put(MediaStore.Downloads.MIME_TYPE, TEMP_FILE_NAME_MIME_TYPE);
                contentValues.put(MediaStore.Downloads.DISPLAY_NAME, TEMP_FILE_NAME);
                contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + TEMP_DIR);

                Uri insert = contentResolver.insert(externalContentUri, contentValues);
                if (insert != null) {
                    OutputStream outputStream = null;
                    try {
                        outputStream = contentResolver.openOutputStream(insert);
                        if (outputStream == null) {
                            return uuid;
                        }
                        outputStream.write(uuid.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } else {
            File externalDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File applicationFileDir = new File(externalDownloadsDir, TEMP_DIR);
            if (!applicationFileDir.exists()) {
                if (!applicationFileDir.mkdirs()) {
                    System.out.println("文件夹创建失败: " + applicationFileDir.getPath());
                }
            }
            File file = new File(applicationFileDir, TEMP_FILE_NAME);
            if (!file.exists()) {
                FileWriter fileWriter = null;
                try {
                    if (file.createNewFile()) {
                        fileWriter = new FileWriter(file, false);
                        fileWriter.write(uuid);
                    } else {
                       System.out.println("文件创建失败：" + file.getPath());
                    }
                } catch (IOException e) {
                    System.out.println("文件创建失败：" + file.getPath());
                    e.printStackTrace();
                } finally {
                    if (fileWriter != null) {
                        try {
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                FileReader fileReader = null;
                BufferedReader bufferedReader = null;
                try {
                    fileReader = new FileReader(file);
                    bufferedReader = new BufferedReader(fileReader);
                    uuid = bufferedReader.readLine();

                    bufferedReader.close();
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return uuid;
    }

    public static String getIMEI(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return null;
            }
            @SuppressLint({"MissingPermission", "HardwareIds"}) String imei = telephonyManager.getDeviceId();
            return imei;
        } catch (Exception e) {
            return null;
        }
    }

}
