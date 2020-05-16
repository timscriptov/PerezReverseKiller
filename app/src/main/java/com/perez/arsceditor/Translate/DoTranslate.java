package com.perez.arsceditor.Translate;

import java.util.List;

import com.memetix.mst.language.Language;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;
import com.perez.arsceditor.ArscActivity;
import com.perez.revkiller.R;

@SuppressWarnings("deprecation")
public class DoTranslate implements OnCheckedChangeListener {

    private Context mContext;

    private CheckBox skip_already_translate;

    private Spinner src_type, translate_to, translator;

    private String[] languages;

    private Language languages_bing[] = { Language.AUTO_DETECT, Language.CHINESE_SIMPLIFIED, Language.ENGLISH,
                                          Language.JAPANESE, Language.KOREAN, Language.FRENCH, Language.THAI, Language.RUSSIAN, Language.GERMAN,
                                          Language.GREEK, Language.ITALIAN, Language.SPANISH, Language.PORTUGUESE, Language.ARABIC
                                        };

    private boolean skip_translate = false;

    private List<String> source_list;

    private List<String> target_list;

    private boolean translate_all = true;

    private int position;

    public DoTranslate(List<String> source_list, List<String> target_list, boolean translate_all, Context context) {

        this.source_list = source_list;

        this.target_list = target_list;
        this.translate_all = translate_all;

        mContext = context;

        languages = context.getResources().getStringArray(R.array.language_short);
    }

    @SuppressLint("InflateParams")
    public void init(int _position) {

        position = _position;
        LayoutInflater factory = LayoutInflater.from(mContext);

        View DialogView = factory.inflate(R.layout.zgtranslate, null);

        new AlertDialog.Builder(mContext).setView(DialogView)
        .setTitle(R.string.translate)
        .setNegativeButton(R.string.translate, new DialogInterface.OnClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(DialogInterface p1, int p2) {

                new translate_task().execute(source_list, target_list);
            }
        }).create()
        .show();

        src_type = (Spinner) DialogView.findViewById(R.id.src_type);

        translate_to = (Spinner) DialogView.findViewById(R.id.translate_to);

        translator = (Spinner) DialogView.findViewById(R.id.translator);

        skip_already_translate = (CheckBox) DialogView.findViewById(R.id.skip_already_translate);

        src_type.setSelection(0);

        translate_to.setSelection(1);

        translator.setSelection(0);

        skip_already_translate.setVisibility(translate_all ? View.VISIBLE : View.GONE);
        skip_already_translate.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton p1, boolean p2) {

        skip_translate = p2;
    }

    class translate_task extends AsyncTask<List<String>, Void, String> {

        private String progress;

        private ProgressDialog pdlg;

        @Override
        protected void onPreExecute() {

            pdlg = new ProgressDialog(mContext);

            pdlg.setTitle(R.string.translating);

            pdlg.setCancelable(false);

            pdlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);

            pdlg.show();
        }

        @Override
        protected String doInBackground(List<String>... parms) {

            String translatedTexte = null;
            try {
                switch(translator.getSelectedItemPosition()) {
                case 1:
                    BingTranslate bingTranslator = new BingTranslate(languages_bing[src_type.getSelectedItemPosition()],
                            languages_bing[translate_to.getSelectedItemPosition()]);
                    if(translate_all) {
                        for(String item : parms[0]) {
                            if(!item.equals(""))

                                translatedTexte = bingTranslator.getTranslateResult(item);
                            else
                                translatedTexte = item;
                            if(skip_translate == true) {
                                if(parms[1].get(parms[0].indexOf(item)).equals("")) {
                                    parms[1].remove(parms[0].indexOf(item));
                                    parms[1].add(parms[0].indexOf(item),
                                                 translatedTexte.equals("") ? item : translatedTexte);
                                }
                            } else {
                                parms[1].remove(parms[0].indexOf(item));
                                parms[1].add(parms[0].indexOf(item),
                                             translatedTexte.equals("") ? item : translatedTexte);
                            }
                            progress = String.valueOf(parms[0].indexOf(item));
                            publishProgress();
                        }
                    } else {
                        if(!parms[0].get(position).equals(""))

                            translatedTexte = bingTranslator.getTranslateResult(parms[0].get(position));
                        else
                            translatedTexte = parms[0].get(position);
                        parms[1].remove(position);
                        parms[1].add(position, translatedTexte.equals("") ? parms[0].get(position) : translatedTexte);
                    }
                    break;
                case 0:
                    BaiduTranslate baiduTranslator = new BaiduTranslate(languages[src_type.getSelectedItemPosition()],
                            languages[translate_to.getSelectedItemPosition()]);
                    if(translate_all) {
                        for(String item : parms[0]) {
                            if(!item.equals(""))

                                translatedTexte = baiduTranslator.getResult(item);
                            else
                                translatedTexte = item;
                            if(skip_translate == true) {
                                if(parms[1].get(parms[0].indexOf(item)).equals("")) {
                                    parms[1].remove(parms[0].indexOf(item));
                                    parms[1].add(parms[0].indexOf(item),
                                                 translatedTexte.equals("") ? item : translatedTexte);
                                }
                            } else {
                                parms[1].remove(parms[0].indexOf(item));
                                parms[1].add(parms[0].indexOf(item),
                                             translatedTexte.equals("") ? item : translatedTexte);
                            }
                            progress = String.valueOf(parms[0].indexOf(item));
                            publishProgress();
                        }
                    } else {
                        if(!parms[0].get(position).equals(""))

                            translatedTexte = baiduTranslator.getResult(parms[0].get(position));
                        else
                            translatedTexte = parms[0].get(position);
                        parms[1].remove(position);
                        parms[1].add(position, translatedTexte.equals("") ? parms[0].get(position) : translatedTexte);
                    }
                    break;
                }

                ((ArscActivity) mContext).isChanged = true;
                return mContext.getString(R.string.translate_success);
            } catch(Exception e) {
                for(String item : parms[1]) {
                    if(item != "") {
                        ((ArscActivity) mContext).isChanged = true;
                        break;
                    }
                }
                return e.toString();
            }
        }

        @Override
        protected void onProgressUpdate(Void... p1) {
            pdlg.setMessage(progress + "/" + source_list.size());
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            pdlg.dismiss();

            ((ArscActivity) mContext).mAdapter.notifyDataSetChanged();
            if(result == mContext.getString(R.string.translate_success))
                Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
            else
                ArscActivity.showMessage(mContext, result).show();
        }
    }

}
