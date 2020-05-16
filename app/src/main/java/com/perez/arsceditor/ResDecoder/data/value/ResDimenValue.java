package com.perez.arsceditor.ResDecoder.data.value;

import android.util.TypedValue;

public class ResDimenValue extends ResIntValue {
    public ResDimenValue(int value, String rawValue) {
        super(value, rawValue, "dimen");
    }

    @Override
    protected String encodeAsResValue() {
        return TypedValue.coerceToString(TypedValue.TYPE_DIMENSION, mValue);
    }
}
