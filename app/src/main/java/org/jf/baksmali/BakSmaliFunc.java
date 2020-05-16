/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.DexFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jf.dexlib2.DexFileFactory;

public class BakSmaliFunc {

    public static final String VERSION;
    public static final int ALL = 1;
    public static final int ALLPRE = 2;
    public static final int ALLPOST = 4;
    public static final int ARGS = 8;
    public static final int DEST = 16;
    public static final int MERGE = 32;
    public static final int FULLMERGE = 64;

    static {
        VERSION = "6.6.6";
    }

    public static boolean DoBaksmali(String infile, String outdir) {
        int registerInfo = 0;
        String outputDirectory = outdir;
        String inputDexFileName = infile;
        String bootClassPath = null;
        StringBuffer extraBootClassPathEntries = new StringBuffer();
        List<String> bootClassPathDirs = new ArrayList<String>();
        bootClassPathDirs.add(".");
        try {
            File dexFileFile = new File(inputDexFileName);
            if(!dexFileFile.exists()) {
                System.err.println("Can't find the file " + inputDexFileName);
                return false;
            }
            //Read in and parse the dex file
            DexFile dexFile = DexFileFactory.loadDexFile(dexFileFile, Opcodes.getDefault());
            String[] bootClassPathDirsArray = new String[bootClassPathDirs.size()];
            for(int i = 0; i < bootClassPathDirsArray.length; i++)
                bootClassPathDirsArray[i] = bootClassPathDirs.get(i);
            BaksmaliOptions bo = new BaksmaliOptions();
            Baksmali.disassembleDexFile(dexFile, new File(outputDirectory), Runtime.getRuntime().availableProcessors(), bo);
        } catch(RuntimeException ex) {
            System.err.println("\n\nUNEXPECTED TOP-LEVEL EXCEPTION:");
            ex.printStackTrace();
            return false;
        } catch(Throwable ex) {
            System.err.println("\n\nUNEXPECTED TOP-LEVEL ERROR:");
            ex.printStackTrace();
            return false;
        }
        return true;
    }

}