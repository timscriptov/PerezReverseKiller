package com.perez.arsceditor.ResDecoder.data;

import java.util.LinkedHashSet;
import java.util.Set;

public class ResValuesFile {
    private final ResType mConfig;
    private final ResPackage mPackage;
    private final Set<ResResource> mResources = new LinkedHashSet<ResResource>();
    private final ResTypeSpec mType;

    public ResValuesFile(ResPackage pkg, ResTypeSpec type, ResType config) {
        this.mPackage = pkg;
        this.mType = type;
        this.mConfig = config;
    }

    public void addResource(ResResource res) {
        mResources.add(res);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        final ResValuesFile other = (ResValuesFile) obj;
        if(this.mType != other.mType && (this.mType == null || !this.mType.equals(other.mType)))
            return false;
        if(this.mConfig != other.mConfig && (this.mConfig == null || !this.mConfig.equals(other.mConfig)))
            return false;
        return true;
    }

    public ResType getConfig() {
        return mConfig;
    }

    public String getPath() {
        return "values" + mConfig.getFlags().getQualifiers() + "/" + mType.getName()
               + (mType.getName().endsWith("s") ? "" : "s") + ".xml";
    }

    public ResTypeSpec getType() {
        return mType;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (this.mType != null ? this.mType.hashCode() : 0);
        hash = 31 * hash + (this.mConfig != null ? this.mConfig.hashCode() : 0);
        return hash;
    }

    public boolean isSynthesized(ResResource res) {
        return mPackage.isSynthesized(res.getResSpec().getId());
    }

    public Set<ResResource> listResources() {
        return mResources;
    }
}
