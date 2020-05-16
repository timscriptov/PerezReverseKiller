package com.perez.revkiller;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.io.OutputStream;

import org.jb.dexlib.*;
import org.jb.dexlib.Code.*;
import org.jb.dexlib.Code.Format.*;
import org.jb.dexlib.Util.*;

public class TypeIdsEditor implements Edit {
    private ArrayList<TypeIdItem> typeIds;

    public void read(List<String> data, byte[] input) throws IOException {
        List<TypeIdItem> typeIds = ClassListActivity.dexFile.TypeIdsSection
                                   .getItems();
        for(TypeIdItem typeId : typeIds)
            data.add(typeId.getTypeDescriptor());
        this.typeIds = (ArrayList) typeIds;
    }

    public void write(String data, OutputStream out) throws IOException {
        ArrayList<TypeIdItem> typeIds = this.typeIds;
        String[] strings = data.split("\n");
        for(int i = 0, len = typeIds.size(); i < len; i++) {
            TypeIdItem item = typeIds.get(i);
            item.setTypeDescriptor(Utf8Utils.escapeSequence(strings[i]));
        }
        ClassListActivity.isChanged = true;
    }
}
