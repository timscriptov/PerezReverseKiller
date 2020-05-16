package com.perez.xml2axml.func;

import android.content.Context;
import android.content.res.AXmlResourceParser;
import android.util.Log;

import com.perez.xml2axml.Encoder;
import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

public class FuncMain {

    public static void encode(Context fc,String in, String out) throws IOException, XmlPullParserException {
        Encoder e = new Encoder();
        byte[] bs = e.encodeFile(fc, in);
        FileUtils.writeByteArrayToFile(new File(out), bs);
    }

    public static void decode(String in, String out) throws FileNotFoundException {
        AXMLPrinter.out = new PrintStream(new File(out));
        AXMLPrinter.bypass(new String[] {in});
        AXMLPrinter.out.close();
    }

    public static boolean isBinAXML(String in) {
        try {
            AXmlResourceParser ass = new AXmlResourceParser();
            ass.open(new FileInputStream(in));
            int type = ass.next();
            Log.d("AXMLJudger","Succeed parsing AXML and type value is"+type);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
