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
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

        /*
        listItems.add(new RecommendProduct_ListItem("1", "나이키", ""));
        listItems.add(new RecommendProduct_ListItem("2", "가죽백", ""));
        listItems.add(new RecommendProduct_ListItem("3", "나이키", ""));
        listItems.add(new RecommendProduct_ListItem("4", "가죽백", ""));
        listItems.add(new RecommendProduct_ListItem("5", "나이키", ""));
        listItems.add(new RecommendProduct_ListItem("6", "가죽백", ""));
        listItems.add(new RecommendProduct_ListItem("7", "나이키", ""));
        listItems.add(new RecommendProduct_ListItem("8", "가죽백", ""));
        listItems.add(new RecommendProduct_ListItem("9", "나이키", ""));
        listItems.add(new RecommendProduct_ListItem("10", "가죽백", ""));
        */

        makeDummyData();
        getRecommendedProductList();

        //RecommendProduct_ListViewAdapter listViewAdapter = new RecommendProduct_ListViewAdapter(this, listItems);

        RecommendProduct_ListViewAdapter listViewAdapter = getRecommendedProductList();

        listview.setAdapter(listViewAdapter);

        // 기준 날짜 셋팅
        TextView recommend_date = (TextView)findViewById(R.id.recommend_date);

        Calendar c = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss 기준");
        String formattedDate = df.format(c.getTime());

        recommend_date.setText(formattedDate);
    }

    public RecommendProduct_ListViewAdapter getRecommendedProductList() {
        SQLiteDatabase db = MainActivity.DBInstance.getReadableDatabase();

        RecommendProduct_ListViewAdapter listViewAdapter = null;
        ArrayList<RecommendProduct_ListItem> listItems = new ArrayList<>();

        ArrayList<String> liked_keywords = new ArrayList<>();

        String SQL = "select DISTINCT " + "K.combination_keyword, M.matching_image_url " +
                     "from " +
                     "searched_product AS S, keyword_in_combination_local AS K, " +
                     "keyword_count_local AS C, matching_combination_local AS M " +
                     "where " + "S.like = 1 " +
                     "AND S.combination_keyword = K.combination_keyword " +
                     "AND K.combination_keyword = M.combination_keyword " +
                     "AND K.keyword_name = C.keyword_name " +
                     "order by C.count DESC";

        Cursor liked_keywords_cursor = db.rawQuery(SQL, null);
        int number = 1;

        if(liked_keywords_cursor != null &&
                liked_keywords_cursor.getCount() != 0)
        {
            liked_keywords_cursor.moveToNext();

            do {
                Log.d(TAG, liked_keywords_cursor.getString(0));
                Log.d(TAG, liked_keywords_cursor.getString(1));

                listItems.add(new RecommendProduct_ListItem(Integer.toString(number),
                        liked_keywords_cursor.getString(0), liked_keywords_cursor.getString(1)));
                number++;
            }while(liked_keywords_cursor.moveToNext());
        }

        db.close();

        listViewAdapter = new RecommendProduct_ListViewAdapter(this, listItems);

        /*
        try
        {

        }catch(IOException ie) {

        }
        */

        return listViewAdapter;
    }

    public void makeDummyData() {
        MainActivity.DBInstance.insertSearchedProduct("nike shoes running air",
                                                      20170527, 1,
                                                       "http://img.wondershoes.co.kr/img/d_14345/1434590/IMG_L.jpg");
        MainActivity.DBInstance.insertSearchedProduct("Starbucks tumbler 400ml stainless",
                                                      20170605, 1,
                                                       "http://ecx.images-amazon.com/images/I/31snxgr7C0L.jpg");
        MainActivity.DBInstance.insertSearchedProduct("woman wallet MCM long black",
                                                      20170402, 1,
                                                       "http://ecx.images-amazon.com/images/I/417u6vjlu5L._AC_UL500_SR500,500_.jpg");

        MainActivity.DBInstance.insertMatchingCombinationLocal("nike shoes running air",
                            "http://img.wondershoes.co.kr/img/d_14345/1434590/IMG_L.jpg");
        MainActivity.DBInstance.insertMatchingCombinationLocal("Starbucks tumbler 400ml stainless",
                "http://ecx.images-amazon.com/images/I/31snxgr7C0L.jpg");
        MainActivity.DBInstance.insertMatchingCombinationLocal("woman wallet MCM long black",
                "http://ecx.images-amazon.com/images/I/417u6vjlu5L._AC_UL500_SR500,500_.jpg");

        MainActivity.DBInstance.insertKeywordInCombinationLocal("nike", "nike shoes running air");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("shoes", "nike shoes running air");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("running", "nike shoes running air");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("air", "nike shoes running air");

        MainActivity.DBInstance.insertKeywordInCombinationLocal("Starbucks", "Starbucks tumbler 400ml stainless");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("tumbler", "Starbucks tumbler 400ml stainless");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("400ml", "Starbucks tumbler 400ml stainless");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("stainless", "Starbucks tumbler 400ml stainless");

        MainActivity.DBInstance.insertKeywordInCombinationLocal("woman", "woman wallet MCM long black");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("wallet", "woman wallet MCM long black");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("MCM", "woman wallet MCM long black");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("long", "woman wallet MCM long black");
        MainActivity.DBInstance.insertKeywordInCombinationLocal("black", "woman wallet MCM long black");

        MainActivity.DBInstance.insertKeywordCountLocal("nike", 5);
        MainActivity.DBInstance.insertKeywordCountLocal("shoes", 40);
        MainActivity.DBInstance.insertKeywordCountLocal("running", 2);
        MainActivity.DBInstance.insertKeywordCountLocal("air", 2);

        MainActivity.DBInstance.insertKeywordCountLocal("Starbucks", 5);
        MainActivity.DBInstance.insertKeywordCountLocal("tumbler", 20);
        MainActivity.DBInstance.insertKeywordCountLocal("400ml", 1);
        MainActivity.DBInstance.insertKeywordCountLocal("stainless", 30);

        MainActivity.DBInstance.insertKeywordCountLocal("woman", 100);
        MainActivity.DBInstance.insertKeywordCountLocal("wallet", 70);
        MainActivity.DBInstance.insertKeywordCountLocal("MCM", 10);
        MainActivity.DBInstance.insertKeywordCountLocal("long", 15);
        MainActivity.DBInstance.insertKeywordCountLocal("black", 200);
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
        private String url;

        public RecommendProduct_ListItem(String number, String keyword, String url) {
            this.number = number;
            this.keyword = keyword;
            this.url = url;
        }

        public String getNumber() { return this.number; }
        public String getKeyword() { return this.keyword; }
        public String getUrl() { return this.url; }

        public void setNumber(String number) { this.number = number; }
        public void setKeyword(String keyword) { this.keyword= keyword; }
        public void setUrl(String url) { this.url = url; }
    }

    /*
        리스트뷰 뷰, xml과 연결된 레이아웃 클래스
     */
    private class RecommendProduct_ListView extends LinearLayout {

        private TextView number_text;
        private TextView keyword_text;
        private LinearLayout listitem_layout;

        public RecommendProduct_ListView(Context context, final RecommendProduct_ListItem aItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.recommend_list_view_item, this, true);

            number_text = (TextView)findViewById(R.id.recommend_product_number_text);
            number_text.setText(aItem.getNumber());

            keyword_text = (TextView)findViewById(R.id.recommend_product_keyword_text);
            keyword_text.setText(aItem.getKeyword());

            listitem_layout = (LinearLayout)findViewById(R.id.recommend_layout);

            listitem_layout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ActivityRecommendProductsSelect.class);
                    intent.putExtra("url", aItem.getUrl());
                    intent.putExtra("keyword", aItem.getKeyword() );
                    startActivityForResult(intent, SELECT_REQUEST);
                }
            });
        }
    }
}