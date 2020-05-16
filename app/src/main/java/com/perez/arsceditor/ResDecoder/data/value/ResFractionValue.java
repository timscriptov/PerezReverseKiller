package com.perez.arsceditor.ResDecoder.data.value;

import android.util.TypedValue;

public class ResFractionValue extends ResIntValue {
    public ResFractionValue(int value, String rawValue) {
        super(value, rawValue, "fraction");
    }

    @Override
    protected String encodeAsResValue() {
        return TypedValue.coerceToString(TypedValue.TYPE_FRACTION, mValue);
    }
}
