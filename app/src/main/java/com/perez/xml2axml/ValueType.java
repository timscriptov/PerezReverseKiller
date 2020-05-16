package com.perez.xml2axml;

public class ValueType {

    public static final  byte NULL = 0x00;

    public static final  byte REFERENCE = 0x01;

    public static final  byte ATTRIBUTE = 0x02;

    public static final  byte STRING = 0x03;

    public static final  byte FLOAT = 0x04;

    public static final  byte DIMENSION = 0x05;

    public static final  byte FRACTION = 0x06;

    public static final  byte FIRST_INT = 0x10;

    public static final  byte INT_DEC = 0x10;

    public static final  byte INT_HEX = 0x11;

    public static final  byte INT_BOOLEAN = 0x12;

    public static final  byte FIRST_COLOR_INT = 0x1c;

    public static final  byte INT_COLOR_ARGB8 = 0x1c;

    public static final  byte INT_COLOR_RGB8 = 0x1d;

    public static final  byte INT_COLOR_ARGB4 = 0x1e;

    public static final  byte INT_COLOR_RGB4 = 0x1f;

    public static final  byte LAST_COLOR_INT = 0x1f;

    public static final  byte LAST_INT = 0x1f;
}
