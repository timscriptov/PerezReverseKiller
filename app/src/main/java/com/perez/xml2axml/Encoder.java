package com.perez.xml2axml;

import android.content.Context;

import com.perez.xml2axml.chunks.Chunk;
import com.perez.xml2axml.chunks.StringPoolChunk;
import com.perez.xml2axml.chunks.TagChunk;
import com.perez.xml2axml.chunks.XmlChunk;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.util.HashSet;

/**
 * Created by Roy on 15-10-4.
 */
public class Encoder {

    public static class Config {
        public static StringPoolChunk.Encoding encoding = StringPoolChunk.Encoding.UNICODE;
        public static int defaultReferenceRadix = 16;
    }

    public byte[] encodeFile(Context context, String filename) throws XmlPullParserException, IOException {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        XmlPullParser p = f.newPullParser();
        p.setInput(new FileInputStream(filename), "UTF-8");
        return encode(context, p);
    }

    public byte[] encodeString(Context context, String xml) throws XmlPullParserException, IOException {
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        XmlPullParser p = f.newPullParser();
        p.setInput(new StringReader(xml));
        return encode(context, p);
    }

    public byte[] encode(Context context, XmlPullParser p) throws XmlPullParserException, IOException {
        XmlChunk chunk = new XmlChunk(context);
        //HashSet<String> strings=new HashSet<String>();
        TagChunk current = null;
        for(int i = p.getEventType(); i != XmlPullParser.END_DOCUMENT; i = p.next()) {
            switch(i) {
            case XmlPullParser.START_DOCUMENT:
                break;
            case XmlPullParser.START_TAG:
                current = new TagChunk(current == null ? chunk : current, p);
                break;
            case XmlPullParser.END_TAG:
                Chunk c = current.getParent();
                current = c instanceof TagChunk ? (TagChunk)c : null;
                break;
            case XmlPullParser.TEXT:
                break;
            default:
                break;
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IntWriter w = new IntWriter(os);
        chunk.write(w);
        w.close();
        return os.toByteArray();
    }
}
