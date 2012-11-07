/*
 * Copyright (C) 2012 Jayfeng.
 */

package com.tianxia.app.healthworld.digest;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.tianxia.app.healthworld.AppApplication;
import com.tianxia.app.healthworld.R;
import com.tianxia.app.healthworld.cache.ConfigCache;
import com.tianxia.app.healthworld.model.ChapterInfo;
import com.tianxia.lib.baseworld.activity.AdapterActivity;
import com.tianxia.lib.baseworld.sync.http.AsyncHttpClient;
import com.tianxia.lib.baseworld.sync.http.AsyncHttpResponseHandler;

public class ChapterListActivity extends AdapterActivity<ChapterInfo>{

    private String title;
    private String url;

    private TextView mAppBannerTextView = null;

    private TextView mItemTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        title = getIntent().getStringExtra("title");
        url = getIntent().getStringExtra("url");

        mAppBannerTextView = (TextView)findViewById(R.id.app_banner_title);
        mAppBannerTextView.setText(title);
        setChapterList();
    }

    @Override
    protected void setLayoutView(){
        setContentView(R.layout.chapter_list_activity);
        setListView(R.id.chapter_list);

        showLoadingEmptyView();
    }

    private void setChapterList() {
         String cacheConfigString = ConfigCache.getUrlCache(AppApplication.mDomain + url);
         if (cacheConfigString != null) {
             showChapterList(cacheConfigString);
         } else {
             AsyncHttpClient client = new AsyncHttpClient();
             client.get(AppApplication.mDomain + url, new AsyncHttpResponseHandler(){

                 @Override
                 public void onStart() {
                 }

                 @Override
                 public void onSuccess(String result){
                     ConfigCache.setUrlCache(result,url);
                     showChapterList(result);
                 }

                 @Override
                 public void onFailure(Throwable arg0) {
                     listView.setAdapter(null);

                     showFailEmptyView();
                 }

             });
         }
    }

    private void showChapterList(String result) {
        try {
            JSONObject chapterConfig = new JSONObject(result);
            JSONArray chapterList = chapterConfig.getJSONArray("chapters");
            ChapterInfo chapterInfo = null;
            for (int i = 0; i < chapterList.length(); i++) {
                chapterInfo = new ChapterInfo();
                chapterInfo.title = chapterList.getJSONObject(i).optString("title");
                chapterInfo.url = chapterList.getJSONObject(i).optString("url");
                chapterInfo.level = 1;
                listData.add(chapterInfo);
                chapterInfo.subChapters = recurseChapters(chapterList.getJSONObject(i).optJSONArray("subChapters"), chapterInfo.level + 1);
            }

            adapter = new Adapter(ChapterListActivity.this);
            listView.setAdapter(adapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private List<ChapterInfo> recurseChapters(JSONArray jsonarray, int level) {
        List<ChapterInfo> result = null;
        if (jsonarray == null || jsonarray.length() == 0) {
            return result;
        }

        result = new ArrayList<ChapterInfo>();
        for (int i = 0; i < jsonarray.length(); i++) {
            ChapterInfo chapterInfo = new ChapterInfo();
            try {
                chapterInfo.title = jsonarray.getJSONObject(i).optString("title");
                chapterInfo.url = jsonarray.getJSONObject(i).optString("url");
                chapterInfo.level = level;
                chapterInfo.subChapters = recurseChapters(jsonarray.getJSONObject(i).optJSONArray("subChapters"), level + 1);
                result.add(chapterInfo);
                listData.add(chapterInfo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected View getView(int position, View convertView) {
        View view = convertView;
        if (listData.get(position).level == 1 && listData.get(position).subChapters != null) {
            view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.chapter_section_list_item, null);
        } else {
            view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.chapter_list_item, null);
        }

        mItemTextView = (TextView) view.findViewById(R.id.item_text);
        if (listData.get(position).subChapters == null) {
            mItemTextView.setText("  ❂ " + listData.get(position).title);
        } else {
            mItemTextView.setText("  " + listData.get(position).title);
        }
        return view;
    }

    protected boolean isItemEnabled(int position) {
        if (listData.get(position).level == 1 && listData.get(position).subChapters != null) {
            return false;
        } else {
            return true;
        }
    }

    protected void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Intent intent = new Intent(this, ChapterDetailsActivity.class);
        intent.putExtra("url", listData.get(position).url);
        startActivity(intent);
    }

}


