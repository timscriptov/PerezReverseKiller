package com.perez.arsceditor.Translate;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

public class BingTranslate {

    private Language SRCLANGUAGE = Language.ENGLISH;

    private Language TARGETLANGUAGE = Language.CHINESE_SIMPLIFIED;

    public BingTranslate(Language from, Language to) {

        SRCLANGUAGE = from;

        TARGETLANGUAGE = to;

        Translate.setClientId("20000227");

        Translate.setClientSecret("bvgP0SOFq1up2Elv2I8QI1Yuhdb0GZlQ82mS0cDohgM=");
        Translate.setHttpReferrer("https://datamarket.azure.com/developer/applications");
    }

    public String getTranslateResult(String str) throws Exception {
        return Translate.execute(str, SRCLANGUAGE, TARGETLANGUAGE);
    }

}
