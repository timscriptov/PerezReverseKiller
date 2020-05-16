package com.perez.xml2axml;

import com.perez.xml2axml.chunks.AttrChunk;
import com.perez.xml2axml.chunks.ValueChunk;

public interface ReferenceResolver {
    int resolve(ValueChunk value, String ref);
}
