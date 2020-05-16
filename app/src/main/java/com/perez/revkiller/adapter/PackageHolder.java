package com.perez.revkiller.adapter;

import android.view.View;
import android.widget.TextView;

import com.perez.revkiller.R;
import com.perez.revkiller.adapter.base.RecyclerViewAdapter;
import com.perez.revkiller.adapter.base.RecyclerViewHolder;

import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;

public class PackageHolder extends RecyclerViewHolder<PackageHolder> {

    TextView textView;

    public PackageHolder(View itemView) {
        super(itemView);
        textView = itemView.findViewById(R.id.title_Name);
    }

    @Override
    public void onBindViewHolder(PackageHolder lineHolder, RecyclerViewAdapter adapter, int position) {
        JNode jPackage = (JNode) adapter.getItem(position);
        lineHolder.textView.setText(jPackage.getName() + ">");
    }
}
