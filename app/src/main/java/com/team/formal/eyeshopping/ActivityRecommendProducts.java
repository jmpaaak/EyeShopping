/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.team.formal.eyeshopping;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class ActivityRecommendProducts extends AppCompatActivity {

    private static final String TAG = ActivityRecommendProducts.class.getSimpleName();

    public static final int SELECT_REQUEST = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_products);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Listview Setting
        ListView listview = (ListView)findViewById(R.id.recommend_product_list_view);

        ArrayList<RecommendProduct_ListItem> listItems = new ArrayList<>();

        listItems.add(new RecommendProduct_ListItem("1", "나이키"));
        listItems.add(new RecommendProduct_ListItem("2", "가죽백"));
        listItems.add(new RecommendProduct_ListItem("3", "나이키"));
        listItems.add(new RecommendProduct_ListItem("4", "가죽백"));
        listItems.add(new RecommendProduct_ListItem("5", "나이키"));
        listItems.add(new RecommendProduct_ListItem("6", "가죽백"));
        listItems.add(new RecommendProduct_ListItem("7", "나이키"));
        listItems.add(new RecommendProduct_ListItem("8", "가죽백"));
        listItems.add(new RecommendProduct_ListItem("9", "나이키"));
        listItems.add(new RecommendProduct_ListItem("10", "가죽백"));

        RecommendProduct_ListViewAdapter listViewAdapter = new RecommendProduct_ListViewAdapter(this, listItems);

        listview.setAdapter(listViewAdapter);

        // 기준 날짜 셋팅
        TextView recommend_date = (TextView)findViewById(R.id.recommend_date);

        Calendar c = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss 기준");
        String formattedDate = df.format(c.getTime());

        recommend_date.setText(formattedDate);

    }


    /*
        리스트 어댑터, 리스트 뷰를 inflate하여 객체화 한다
     */
    private class RecommendProduct_ListViewAdapter extends BaseAdapter {
        Context context;
        ArrayList<RecommendProduct_ListItem> listItems;

        private RecommendProduct_ListViewAdapter(Context context, ArrayList<RecommendProduct_ListItem> listItems) {
            this.context = context;
            this.listItems = listItems;
        }

        public int getCount() {
            return listItems.size();
        }

        public RecommendProduct_ListItem getItem(int position) {
            return listItems.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RecommendProduct_ListView listView;

            if(convertView == null) {
                listView = new RecommendProduct_ListView(this.context, this.listItems.get(position));
            }else {
                listView = (RecommendProduct_ListView) convertView;
            }

            return listView;
        }
    }

    /*
        리스트뷰 아이템, 리스트 뷰 항목 하나 하나에 들어갈 정보를 담고 있다
     */
    private class RecommendProduct_ListItem {
        private String number;
        private String keyword;

        public RecommendProduct_ListItem(String number, String keyword) {
            this.number = number;
            this.keyword = keyword;
        }

        public String getNumber() { return this.number; }
        public String getKeyword() { return this.keyword; }

        public void setNumber(String number) { this.number = number; }
        public void setKeyword(String keyword) { this.keyword= keyword; }
    }

    /*
        리스트뷰 뷰, xml과 연결된 레이아웃 클래스
     */
    private class RecommendProduct_ListView extends LinearLayout {

        private TextView number_text;
        private TextView keyword_text;

        public RecommendProduct_ListView(Context context, final RecommendProduct_ListItem aItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.recommend_list_view_item, this, true);

            number_text = (TextView)findViewById(R.id.recommend_product_number_text);
            number_text.setText(aItem.getNumber());

            keyword_text = (TextView)findViewById(R.id.recommend_product_keyword_text);
            keyword_text.setText(aItem.getKeyword());
        }
    }
}