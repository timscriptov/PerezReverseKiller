package com.perez.revkiller;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.perez.revkiller.adapter.ClassAdapter;
import com.perez.revkiller.adapter.ClassHolder;
import com.perez.revkiller.adapter.PackageAdapter;
import com.perez.revkiller.adapter.base.RecyclerViewAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.core.utils.exceptions.JadxException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JSources;

public class PackageActivity extends AppCompatActivity {

    private RecyclerView title_recycler_view;
    private RecyclerView recyclerView;
    private ClassAdapter classAdapter;
    private List<JNode> jNodes = new ArrayList<>();
    private LinearLayout empty_rel;
    private PackageAdapter titleAdapter;
    private String tmpFile;
    private JadxDecompiler decompiler;
    ProgressDialog progressDialog;

    private ProgressDialog showWaitingDialog() {
        /* 等待Dialog具有屏蔽其他控件的交互能力
         * @setCancelable 为使屏幕不可点击，设置为不可取消(false)
         * 下载等事件完成后，主动调用函数关闭该Dialog
         */
        ProgressDialog waitingDialog =
            new ProgressDialog(PackageActivity.this);
        waitingDialog.setMessage("加载中...");
        waitingDialog.setIndeterminate(true);
        waitingDialog.setCancelable(false);
        waitingDialog.show();
        return waitingDialog;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.package_menu, menu);
        return true;
    }

    private ProgressDialog getProgressDialog(Context context, String title) {
        /* @setProgress 设置初始进度
         * @setProgressStyle 设置样式（水平进度条）
         * @setMax 设置进度最大值
         */
        final int MAX_PROGRESS = 100;
        final ProgressDialog progressDialog =
            new ProgressDialog(context);
        progressDialog.setProgress(0);
        progressDialog.setTitle(title);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(MAX_PROGRESS);
        progressDialog.show();
        return progressDialog;
    }

    public void deCode(String fileName) {
        JadxArgs args = new JadxArgs();
        args.setOutDir(new File(fileName.substring(0, fileName.lastIndexOf(".")) + "_src"));
        args.setInputFile(new File(fileName));
        args.setThreadsCount(Runtime.getRuntime().availableProcessors());
        final JadxDecompiler decompiler = new JadxDecompiler(args);
        try {
            decompiler.load();
        } catch(Exception e) {
            Toast.makeText(PackageActivity.this, "错误信息：" + e.toString(), Toast.LENGTH_SHORT).show();
            return;
        }
        new JSources(decompiler);
        final ProgressDialog progressDialog = getProgressDialog(PackageActivity.this, "反编译进度");
        Runnable save = new Runnable() {
            @Override
            public void run() {
                try {
                    ThreadPoolExecutor ex = (ThreadPoolExecutor) decompiler.getSaveExecutor();
                    ex.shutdown();
                    while(ex.isTerminating()) {
                        long total = ex.getTaskCount();
                        long done = ex.getCompletedTaskCount();
                        progressDialog.setProgress((int)(done * 100.0 / (double) total));
                        Thread.sleep(300);
                    }
                    progressDialog.cancel();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PackageActivity.this, "反编译完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch(InterruptedException e) {
                    Toast.makeText(PackageActivity.this, "错误信息：" + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        new Thread(save).start();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.mass_decompile:
            deCode(getIntent().getStringExtra("fileName"));
            break;
        default:
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        title_recycler_view = findViewById(R.id.title_recycler_view);
        title_recycler_view.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        titleAdapter = new PackageAdapter(this, new ArrayList<JNode>());
        title_recycler_view.setAdapter(titleAdapter);
        recyclerView = findViewById(R.id.recycler_view);
        classAdapter = new ClassAdapter(this, jNodes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(classAdapter);
        empty_rel = findViewById(R.id.empty_rel);
        classAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, RecyclerView.ViewHolder viewHolder, int position) {
                if(viewHolder instanceof ClassHolder) {
                    JNode jNode = jNodes.get(position);
                    if(R.mipmap.package_obj == jNode.getIcon()) {
                        JPackage jPackage = (JPackage) jNode;
                        getFile(jPackage);
                        refreshTitleState(jPackage);
                    } else if(R.mipmap.package_obj != jNode.getIcon()) {
                        JClass jClass = (JClass) jNode;
                        ArrayList<String> info = new ArrayList<>();
                        info.add(jClass.getName() + ".java");
                        info.add(jClass.getCls().getCode());
                        DecompileFileUtil.openDecodeIntent(PackageActivity.this, info);
                        Toast.makeText(PackageActivity.this, ((JClass) jNode).getFullName(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        titleAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, RecyclerView.ViewHolder viewHolder, int position) {
                JNode jPackage = (JNode) titleAdapter.getItem(position);
                getFile(jPackage);
                int count = titleAdapter.getItemCount();
                int removeCount = count - position - 1;
                for(int i = 0; i < removeCount; i++)
                    titleAdapter.removeLast();
            }
        });
        progressDialog = showWaitingDialog();
        final String fileName = getIntent().getStringExtra("fileName");
        File inputFile = new File(fileName);
        setTitle(inputFile.getName());
        JadxArgs args = new JadxArgs();
        args.setSkipResources(true);
        args.setShowInconsistentCode(true);
        args.setInputFile(new File(fileName));
        decompiler = new JadxDecompiler(args);
        new Thread() {
            @Override
            public void run() {
                try {
                    decompiler.load();
                } catch(Exception e) {
                //    Toast.makeText(PackageActivity.this, "错误信息：" + e.toString(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                    finish();
                }
                Message msg = new Message();
                msg.what = 1;
                mHandler.sendMessage(msg);
            }
        } .start();
    }

    public void open() {
        JSources jSources = new JSources(decompiler);
        refreshTitleState(jSources);
        getFile(jSources);
    }

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            progressDialog.cancel();
            open();
            Toast.makeText(PackageActivity.this, "加载完成", Toast.LENGTH_LONG).show();
            //Toast.makeText(FileActivity.this, "运行时间：" + msg.obj + "s", Toast.LENGTH_LONG).show();
        }
    };

    void refreshTitleState(JNode jPackage) {
        titleAdapter.addItem(jPackage);
        //title_recycler_view.smoothScrollToPosition(titleAdapter.getItemCount());
    }

    public void getFile(JNode jNode) {
        new PackageActivity.MyTask(jNode).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    class MyTask extends AsyncTask {
        JNode jNode;

        MyTask(JNode jNode) {
            this.jNode = jNode;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            List jNodes = new ArrayList<>();
            if(R.mipmap.packagefolder_obj == jNode.getIcon()) {
                JSources jSources = (JSources) jNode;
                jNodes = jSources.getRootPackage();
            } else if(R.mipmap.package_obj == jNode.getIcon()) {
                if(jNode != null) {
                    JPackage jPackage = (JPackage) jNode;
                    jNodes.addAll(jPackage.getInnerPackages());
                    jNodes.addAll(jPackage.getClasses());
                }
            }
            PackageActivity.this.jNodes = jNodes;
            return jNodes;
        }

        @Override
        protected void onPostExecute(Object o) {
            if(jNodes.size() > 0)
                empty_rel.setVisibility(View.GONE);
            else
                empty_rel.setVisibility(View.VISIBLE);
            classAdapter.refresh(jNodes);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            List<JNode> jNodes = (List<JNode>) titleAdapter.getAdapterData();
            if(jNodes.size() == 1)
                finish();
            else {
                titleAdapter.removeItem(jNodes.size() - 1);
                getFile(jNodes.get(jNodes.size() - 1));
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
