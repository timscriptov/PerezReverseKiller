package com.perez.elfeditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UnknownFormatConversionException;

import com.perez.elfeditor.IO.LEDataInputStream;
import com.perez.elfeditor.IO.LEDataOutputStream;

import android.annotation.SuppressLint;
import com.perez.revkiller.*;

public class Elf implements Closeable {

    public static abstract class Ehdr {
        short e_type;
        short e_machine;
        int e_version;

        int e_flags;
        short e_ehsize;
        short e_phentsize;
        short e_phnum;
        short e_shentsize;
        short e_shnum;
        short e_shstrndx;

        abstract long getProgramOffset();

        abstract long getSectionOffset();
    }

    static abstract class Elf_Phdr {
        int p_type;
        int p_offset;

        String flagsString() {
            return "(" + ((getFlags() & PF_R) != 0 ? "R" : "_") + ((getFlags() & PF_W) != 0 ? "W" : "_")
                   + ((getFlags() & PF_X) != 0 ? "X" : "_") + ")";
        }

        abstract long getFlags();

        String programType() {
            switch(p_type) {
            case PT_NULL:
                return "NULL";
            case PT_LOAD:
                return "Loadable Segment";
            case PT_DYNAMIC:
                return "Dynamic Segment";
            case PT_INTERP:
                return "Interpreter Path";
            case PT_NOTE:
                return "Note";
            case PT_SHLIB:
                return "PT_SHLIB";
            case PT_PHDR:
                return "Program Header";
            default:
                return "Unknown Section";
            }
        }
    }

    public static abstract class Elf_Shdr {
        int sh_name;
        int sh_type;
        int sh_link;
        int sh_info;
        int index;

        public abstract long getOffset();

        public abstract int getSize();
    }

    static abstract class Elf_Sym {
        int st_name;
        char st_info;
        char st_other;
        short st_shndx;

        char getBinding() {
            return (char)(st_info >> 4);
        }

        abstract long getSize();

        char getType() {
            return (char)(st_info & 0x0f);
        }

        void setBinding(char b) {
            setBindingAndType(b, getType());
        }

        void setBindingAndType(char b, char t) {
            st_info = (char)((b << 4) + (t & 0x0f));
        }

        void setType(char t) {
            setBindingAndType(getBinding(), t);
        }
    }

    static class Elf32_Ehdr extends Ehdr {
        int e_entry;
        int e_phoff;
        int e_shoff;

        @Override
        long getProgramOffset() {
            return e_phoff;
        }

        @Override
        long getSectionOffset() {
            return e_shoff;
        }
    }

    static class Elf32_Phdr extends Elf_Phdr {
        int p_vaddr;
        int p_paddr;
        int p_filesz;
        int p_memsz;
        int p_flags;
        int p_align;

        @Override
        public long getFlags() {
            return p_flags;
        }
    }

    static class Elf32_Shdr extends Elf_Shdr {
        int sh_flags;
        int sh_addr;
        int sh_offset;
        int sh_size;
        int sh_addralign;
        int sh_entsize;

        @Override
        public long getOffset() {
            return sh_offset;
        }

        @Override
        public int getSize() {
            return sh_size;
        }
    }

    static class Elf32_Sym extends Elf_Sym {
        int st_value;
        int st_size;

        @Override
        long getSize() {
            return st_size;
        }
    }

    static class Elf64_Ehdr extends Ehdr {
        long e_entry;
        long e_phoff;
        long e_shoff;

        @Override
        long getProgramOffset() {
            return e_phoff;
        }

        @Override
        long getSectionOffset() {
            return e_shoff;
        }
    }

    static class Elf64_Phdr extends Elf_Phdr {
        long p_vaddr;
        long p_paddr;
        long p_filesz;
        long p_memsz;
        long p_flags;
        long p_align;

        @Override
        public long getFlags() {
            return p_flags;
        }
    }

    static class Elf64_Shdr extends Elf_Shdr {
        long sh_flags;
        long sh_addr;
        long sh_offset;
        long sh_size;
        long sh_addralign;
        long sh_entsize;

        @Override
        public long getOffset() {
            return sh_offset;
        }

        @Override
        public int getSize() {
            return (int) sh_size;
        }
    }

    static class Elf64_Sym extends Elf_Sym {
        long st_value;
        long st_size;

        @Override
        long getSize() {
            return st_size;
        }
    };

    public static class ItemHelper {
        public String oldval;
        public String newVal;
        public int sym_offset;
        public byte[] data;

        public ItemHelper() {
        }

        public ItemHelper(String val) {
            this.oldval = val;
        }

        @Override
        public boolean equals(Object object) {
            return oldval.equals(((ItemHelper) object).oldval);
        }

        @Override
        public int hashCode() {
            return oldval.hashCode();
        }
    };

    final static char ElfMagic[] = { 0x7f, 'E', 'L', 'F', '\0' };
    final static int EI_CLASS = 4;
    final static int EI_DATA = 5;
    final static int EI_NIDENT = 16;

    public static final String SHN_DYNSYM = ".dynsym";
    public static final String SHN_DYNSTR = ".dynstr";
    public static final String SHN_HASH = ".hash";

    public static final String SHN_RODATA = ".rodata";

    public static final String SHN_TEXT = ".text";
    public static final String SHN_DYNAMIC = ".dynamic";
    public static final String SHN_SHSTRTAB = ".shstrtab";

    final static int SHN_UNDEF = 0;

    final static int SHT_PROGBITS = 1;
    final static int SHT_SYMTAB = 2;
    final static int SHT_STRTAB = 3;

    final static int SHT_RELA = 4;

    final static int SHT_HASH = 5;

    final static int SHT_DYNAMIC = 6;

    final static int SHT_DYNSYM = 11;

    final static int PT_NULL = 0;

    final static int PT_LOAD = 1;

    final static int PT_DYNAMIC = 2;
    final static int PT_INTERP = 3;
    final static int PT_NOTE = 4;
    final static int PT_SHLIB = 5;
    final static int PT_PHDR = 6;
    final static int PT_TLS = 7;

    final static int PF_X = 1;
    final static int PF_W = 2;

    final static int PF_R = 4;
    final static int PF_MASKOS = 0x0ff00000;

    final static int PF_MASKPROC = 0xf0000000;

    @SuppressWarnings("resource")
    public static boolean cloneElf(ByteArrayInputStream bis, OutputStream os, String packageName_O,
                                   String packageName_N) throws UnknownFormatConversionException, IOException {
        Elf elf = new Elf(bis);
        if(elf.error) {
            elf.close();
            return false;
        }

        for(ItemHelper item : elf.dy_items) {
            if(item.oldval.contains(packageName_O)) {
                String n = "Java_" + packageName_N;
                String s = "Java_" + packageName_O;
                item.newVal = n + item.oldval.substring(s.length());
            }
        }
        if(elf.ro_items != null) {
            packageName_O = packageName_O.replace("_", "/");
            packageName_N = packageName_N.replace("_", "/");

            for(ItemHelper item : elf.ro_items) {
                if(item.oldval.contains(packageName_O)) {
                    String start = item.oldval.substring(0, item.oldval.indexOf(packageName_O));
                    item.newVal = start + packageName_N
                                  + item.oldval.substring(packageName_N.length() + start.length());
                }
            }
        }
        elf.writeELF(os);
        return !elf.error;
    }

    public static boolean isElf(File f) {
        long n = 0;
        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            n = raf.readInt();
            raf.close();
        } catch(IOException ex) {
            System.out.println(ex);
        }
        return n == 0x7F454C46;
    }

    public static byte[] readFile(File file) throws FileNotFoundException, IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = new FileInputStream(file);
        byte buffer[] = new byte[2048];
        int count;
        while((count = is.read(buffer)) != -1)
            bos.write(buffer, 0, count);
        is.close();
        return bos.toByteArray();
    }

    public List<ItemHelper> dy_items, ro_items;

    final byte[] e_ident = new byte[EI_NIDENT];
    private LEDataInputStream mReader;

    private final Ehdr mHeader;

    private final Elf_Shdr[] mSectionHeaders;

    private byte[] mStringTable;

    private byte mRoDataStringTable[];

    Elf_Phdr[] mProgHeaders;
    Elf_Sym[] mDynamicSymbols;
    Elf_Sym[] mHashSymbols;

    byte[] mDynStringTable;

    byte[] mDynHashTable;

    private int num_buckets;

    private int num_chains;

    private boolean error;

    public Elf(ByteArrayInputStream bis) throws IOException, UnknownFormatConversionException {
        dy_items = new ArrayList<ItemHelper>();
        final LEDataInputStream r = mReader = new LEDataInputStream(bis);
        r.readFully(e_ident);
        if(!checkMagic())
            throw new UnknownFormatConversionException("Invalid elf magic");
        r.setIsLittleEndian(isLittleEndian());
        final boolean is64bit = is64bit();
        if(is64bit) {
            Elf64_Ehdr header = new Elf64_Ehdr();
            header.e_type = r.readShort();
            header.e_machine = r.readShort();
            header.e_version = r.readInt();
            header.e_entry = r.readLong();
            header.e_phoff = r.readLong();
            header.e_shoff = r.readLong();
            mHeader = header;
        } else {
            Elf32_Ehdr header = new Elf32_Ehdr();
            header.e_type = r.readShort();
            header.e_machine = r.readShort();
            header.e_version = r.readInt();
            header.e_entry = r.readInt();
            header.e_phoff = r.readInt();
            header.e_shoff = r.readInt();
            mHeader = header;
        }
        final Ehdr h = mHeader;
        h.e_flags = r.readInt();
        h.e_ehsize = r.readShort();
        h.e_phentsize = r.readShort();
        h.e_phnum = r.readShort();
        h.e_shentsize = r.readShort();
        h.e_shnum = r.readShort();
        h.e_shstrndx = r.readShort();
        mSectionHeaders = new Elf_Shdr[h.e_shnum];
        for(int i = 0; i < h.e_shnum; i++) {
            final long offset = h.getSectionOffset() + (i * h.e_shentsize);

            r.seek(offset);
            if(is64bit) {
                Elf64_Shdr secHeader = new Elf64_Shdr();
                secHeader.sh_name = r.readInt();
                secHeader.sh_type = r.readInt();
                secHeader.sh_flags = r.readLong();
                secHeader.sh_addr = r.readLong();
                secHeader.sh_offset = r.readLong();
                secHeader.sh_size = r.readLong();
                secHeader.sh_link = r.readInt();
                secHeader.sh_info = r.readInt();
                secHeader.sh_addralign = r.readLong();
                secHeader.sh_entsize = r.readLong();
                secHeader.index = i;
                mSectionHeaders[i] = secHeader;
            } else {
                Elf32_Shdr secHeader = new Elf32_Shdr();
                secHeader.sh_name = r.readInt();
                secHeader.sh_type = r.readInt();
                secHeader.sh_flags = r.readInt();
                secHeader.sh_addr = r.readInt();
                secHeader.sh_offset = r.readInt();
                secHeader.sh_size = r.readInt();
                secHeader.sh_link = r.readInt();
                secHeader.sh_info = r.readInt();
                secHeader.sh_addralign = r.readInt();
                secHeader.sh_entsize = r.readInt();
                secHeader.index = i;
                mSectionHeaders[i] = secHeader;
            }
        }
        if(h.e_shstrndx > -1 && h.e_shstrndx < mSectionHeaders.length) {
            Elf_Shdr strSec = mSectionHeaders[h.e_shstrndx];

            if(strSec.sh_type == SHT_STRTAB) {
                int strSecSize = strSec.getSize();
                mStringTable = new byte[strSecSize];
                r.seek(strSec.getOffset());
                r.readFully(mStringTable);
                for(Elf_Shdr sec : mSectionHeaders) {

                    System.out.println(getString(sec.sh_name));
                }
            } else
                throw new UnknownFormatConversionException("Wrong string section e_shstrndx=" + h.e_shstrndx);
        } else
            throw new UnknownFormatConversionException("Invalid e_shstrndx=" + h.e_shstrndx);
        try {
            if(!readMore(dy_items))
                error = true;
        } catch(OutOfMemoryError e) {
            error = true;
        }
    }

    public Elf(ByteArrayInputStream bis, ResourceCallBack callBack)
    throws IOException, UnknownFormatConversionException {
        this(bis);
        for(ItemHelper item : this.dy_items) {
            ResourceHelper helper = new ResourceHelper();
            helper.VALUE = item.oldval;
            helper.TYPE = "dynstr";
            callBack.back(helper);
        }
        if(this.mRoDataStringTable != null) {
            for(ItemHelper item : this.ro_items) {
                ResourceHelper helper = new ResourceHelper();
                helper.VALUE = item.oldval;
                helper.TYPE = "rodata";
                callBack.back(helper);
            }
        }
    }

    public Elf(File file) throws IOException, UnknownFormatConversionException {
        this(new ByteArrayInputStream(readFile(file)));
    }

    public Elf(String file) throws IOException, UnknownFormatConversionException {
        this(new File(file));
    }

    public Elf(String file, boolean closeNow) throws IOException, UnknownFormatConversionException {
        this(file);
        if(closeNow)
            mReader.close();
    }

    final boolean checkMagic() {
        return e_ident[0] == ElfMagic[0];
    }

    @Override
    public void close() throws IOException {
        mReader.close();
    }

    private String fillString(String string, int length) {
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        if(string.length() < length) {
            int remaining = length - string.length();
            while(remaining-- > 0)
                sb.append(" ");
        }
        return sb.toString();
    }

    final byte getDataEncoding() {
        return e_ident[EI_DATA];
    }

    public final String getDynString(int index) {
        if(index == SHN_UNDEF)
            return "SHN_UNDEF";
        int start = index;
        int end = index;
        while(mDynStringTable[end] != '\0')
            end++;
        return new String(mDynStringTable, start, end - start);
    }

    final byte getFileClass() {
        return e_ident[EI_CLASS];
    }

    public Ehdr getHeader() {
        return mHeader;
    }

    public LEDataInputStream getReader() {
        return mReader;
    }

    public final Elf_Shdr getSectionByName(String name) {
        for(Elf_Shdr sec : mSectionHeaders) {
            if(name.equals(getString(sec.sh_name)))
                return sec;
        }
        return null;
    }

    public Elf_Shdr[] getSectionHeaders() {
        return mSectionHeaders;
    }

    public final String getString(int index) {
        if(index == SHN_UNDEF)
            return "SHN_UNDEF";
        int start = index;
        int end = index;
        while(mStringTable[end] != '\0')
            end++;
        return new String(mStringTable, start, end - start);
    }

    public final boolean is64bit() {
        return getFileClass() == 2;
    }

    public final boolean isLittleEndian() {
        return getDataEncoding() == 1;
    }

    private boolean readMore(List<ItemHelper> items) throws IOException {
        final Ehdr h = mHeader;
        final LEDataInputStream r = mReader;
        final boolean is64bit = is64bit();
        Elf_Shdr dynsym = getSectionByName(SHN_DYNSYM);
        if(dynsym != null) {
            r.seek(dynsym.getOffset());
            int len = dynsym.getSize() / (is64bit ? 24 : 16);

            mDynamicSymbols = new Elf_Sym[len];
            for(int i = 0; i < len; i++) {
                if(is64bit) {
                    Elf64_Sym dsym = new Elf64_Sym();
                    dsym.st_name = r.readInt();
                    dsym.st_info = (char) r.readByte();
                    dsym.st_other = (char) r.readByte();
                    dsym.st_value = r.readLong();
                    dsym.st_size = r.readLong();
                    dsym.st_shndx = r.readShort();
                    mDynamicSymbols[i] = dsym;
                } else {
                    Elf32_Sym dsym = new Elf32_Sym();
                    dsym.st_name = r.readInt();
                    dsym.st_value = r.readInt();
                    dsym.st_size = r.readInt();
                    dsym.st_info = (char) r.readByte();
                    dsym.st_other = (char) r.readByte();
                    dsym.st_shndx = r.readShort();
                    mDynamicSymbols[i] = dsym;
                }
            }
            Elf_Shdr dynLinkSec = mSectionHeaders[dynsym.sh_link];
            r.seek(dynLinkSec.getOffset());
            mDynStringTable = new byte[dynLinkSec.getSize()];
            r.readFully(mDynStringTable);
            String string = new String(mDynStringTable);
            String[] mDyStrs = string.split("\0");
            int index = 1;
            for(String mDyStr : mDyStrs) {
                if(mDyStr.length() == 0)
                    continue;
                ItemHelper item = new ItemHelper();
                item.oldval = mDyStr;
                if(mDyStr.startsWith("lib") && mDyStr.endsWith(".so"))
                    item.sym_offset = -1;
                else
                    item.sym_offset = index++;
                items.add(item);
            }
        }
        Elf_Shdr dyhash = getSectionByName(SHN_HASH);
        if(dyhash != null) {
            r.seek(dyhash.getOffset());
            num_buckets = r.readInt();
            num_chains = r.readInt();
            r.readIntArray(num_buckets);
            r.readIntArray(num_chains);
            int actual = num_buckets * 4 + num_chains * 4 + 8;
            if(dyhash.getSize() != actual) {
                throw new IOException("Error reading string table (read " + actual + "bytes, expected to " + "read "
                                      + dyhash.getSize() + "bytes).");
            }
        }
        mProgHeaders = new Elf_Phdr[h.e_phnum];
        for(int i = 0; i < h.e_phnum; i++) {
            final long offset = h.getProgramOffset() + (i * h.e_phentsize);
            r.seek(offset);
            if(is64bit) {
                Elf64_Phdr progHeader = new Elf64_Phdr();
                progHeader.p_type = r.readInt();
                progHeader.p_offset = r.readInt();
                progHeader.p_vaddr = r.readLong();
                progHeader.p_paddr = r.readLong();
                progHeader.p_filesz = r.readLong();
                progHeader.p_memsz = r.readLong();
                progHeader.p_flags = r.readLong();
                progHeader.p_align = r.readLong();
                mProgHeaders[i] = progHeader;
            } else {
                Elf32_Phdr progHeader = new Elf32_Phdr();
                progHeader.p_type = r.readInt();
                progHeader.p_offset = r.readInt();
                progHeader.p_vaddr = r.readInt();
                progHeader.p_paddr = r.readInt();
                progHeader.p_filesz = r.readInt();
                progHeader.p_memsz = r.readInt();
                progHeader.p_flags = r.readInt();
                progHeader.p_align = r.readInt();
                mProgHeaders[i] = progHeader;
            }
        }
        Elf_Shdr roData = getSectionByName(SHN_RODATA);
        if(roData != null) {
            r.seek(roData.getOffset());
            mRoDataStringTable = new byte[roData.getSize()];

            r.readFully(mRoDataStringTable);
            ro_items = new ArrayList<ItemHelper>();
            int end = 0;
            while(end != mRoDataStringTable.length) {
                while(end != mRoDataStringTable.length && mRoDataStringTable[end++] == 0)
                    ;
                int start = end;
                while(end != mRoDataStringTable.length && mRoDataStringTable[end++] != 0)
                    ;
                ItemHelper item = new ItemHelper();
                item.oldval = new String(mRoDataStringTable, start - 1, end - start);
                item.data = new byte[end - start];
                System.arraycopy(mRoDataStringTable, start - 1, item.data, 0, item.data.length);
                ro_items.add(item);
            }
        }
        return true;
    }

    static {
        try {
            System.loadLibrary("function");
        } catch(UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public long ELFHash(String strUri) {
        return Features.ELFHash(strUri);
    }

    private final void writeDynHash(List<ItemHelper> items, LEDataOutputStream lmOut) throws IOException {
        lmOut.writeInt(num_buckets);
        lmOut.writeInt(num_chains);
        int buckets_t[] = new int[num_buckets];
        int chains_t[] = new int[num_chains];
        for(ItemHelper item : items) {

            int offset = (int)(ELFHash(item.newVal == null ? item.oldval : item.newVal) % num_buckets);
            if(item.sym_offset == -1)
                continue;
            if(buckets_t[offset] == 0)
                buckets_t[offset] = item.sym_offset;
            else {
                for(offset = buckets_t[offset]; offset != 0; offset = chains_t[offset]) {
                    if(chains_t[offset] == 0) {
                        chains_t[offset] = item.sym_offset;
                        break;
                    }
                }
            }
        }
        lmOut.writeIntArray(buckets_t);
        lmOut.writeIntArray(chains_t);
    }

    private final long writeDynString(List<ItemHelper> items, LEDataOutputStream lmOut) throws IOException {
        long offset = 0;
        long len = 0;
        for(ItemHelper item : items) {
            String old_string = item.oldval;
            if(old_string.equals("SHN_UNDEF"))
                continue;

            lmOut.writeByte((byte) '\0');
            byte data[] = old_string.getBytes();
            offset += data.length;
            if(item.newVal != null) {
                item.newVal = fillString(item.newVal, item.oldval.length());
                data = item.newVal.getBytes();
            }
            len += data.length;
            lmOut.writeFully(data);
            offset += 1;
            len += 1;
        }

        if(mDynStringTable.length - (int) offset <= 0)
            return len;
        lmOut.writeFully(mDynStringTable, (int) offset, mDynStringTable.length - (int) offset);
        len += mDynStringTable.length - (int) offset;
        return len;
    }

    public final void writeELF(OutputStream os) throws IOException {
        final LEDataOutputStream lmOut = new LEDataOutputStream(os);
        Elf_Shdr dynsym = getSectionByName(SHN_DYNSYM);
        Elf_Shdr dynLinkSec = mSectionHeaders[dynsym.sh_link];
        long offset = dynLinkSec.getOffset();
        Elf_Shdr dyhash = getSectionByName(SHN_HASH);
        long offset2 = dyhash.getOffset();

        if(offset > offset2) {
            writeExtra(0, offset2, lmOut);
            writeDynHash(dy_items, lmOut);
            offset2 += num_buckets * 4 + num_chains * 4 + 8;
            writeExtra(offset2, offset, lmOut);
            offset2 = offset;
            writeDynString(dy_items, lmOut);
            offset2 += mDynStringTable.length;
            offset = offset2;
        } else {
            writeExtra(0, offset, lmOut);
            writeDynString(dy_items, lmOut);
            offset += mDynStringTable.length;
            writeExtra(offset, offset2, lmOut);
            offset = offset2;
            writeDynHash(dy_items, lmOut);
            offset += num_buckets * 4 + num_chains * 4 + 8;
        }

        Elf_Shdr roData = getSectionByName(SHN_RODATA);
        if(roData != null) {
            writeExtra(offset, roData.getOffset(), lmOut);
            offset = roData.getOffset();
            writeRodataBytes();
            lmOut.writeFully(mRoDataStringTable);
            offset += mRoDataStringTable.length;
        }
        writeExtra(offset, mReader.size, lmOut);
        lmOut.close();
        close();
    }

    private void writeExtra(long offset1, long offset2, LEDataOutputStream lmOut) throws IOException {
        long len = offset2 - offset1;
        if(len <= 0)
            return;
        int buf_len = 2048;
        long remaining = len;
        mReader.seek(offset1);
        byte buffer[] = new byte[buf_len];
        while(remaining != 0) {
            if(buf_len <= remaining) {
                mReader.readFully(buffer);
                lmOut.writeFully(buffer);
                remaining -= buf_len;
            } else {
                mReader.readFully(buffer, 0, (int) remaining);
                lmOut.writeFully(buffer, 0, (int) remaining);
                remaining = 0;
            }
        }
    }

    @SuppressLint("DefaultLocale")
    public void sortStrData(List<String> source, List<String> target, List<ItemHelper> items) {
        int index = 0;
        for(String string : target) {
            if(!string.equals("")) {
                int i = items.indexOf(new ItemHelper(source.get(index)));
                if(i == -1)
                    continue;
                ItemHelper item = items.get(i);
                item.newVal = string;
            }
            index++;
        }
    }

    public static int findBytesPos(byte[] data, byte[] found) {
        for(int i = 0; i < data.length; i++) {
            boolean bFound = true;
            for(int j = 0; j < found.length; j++)
                bFound = bFound && data[i + j] == found[j];
            if(bFound)
                return i;
        }
        return -1;
    }

    public void writeRodataBytes() throws UnsupportedEncodingException {
        for(ItemHelper item : ro_items) {
            if(item.newVal != null && !item.newVal.equals("")) {
                byte[] s_data = item.data;

                int pos = findBytesPos(mRoDataStringTable, s_data);
                if(pos == -1)
                    continue;

                byte[] data = item.newVal.getBytes();
                for(byte b : data)
                    mRoDataStringTable[pos++] = b;
                int len_s = s_data.length - data.length;
                while(len_s-- > 0)
                    mRoDataStringTable[pos++] = 20;
            }
        }
    }
}
