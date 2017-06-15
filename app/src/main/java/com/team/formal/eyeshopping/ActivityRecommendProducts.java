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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import static com.team.formal.eyeshopping.MainActivity.DBInstance;

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

        makeLocalDummyData();
        //makeServerDummyData();

        // Listview Setting
        getRecommendedProductList();

        // 기준 날짜 셋팅
        TextView recommend_date = (TextView)findViewById(R.id.recommend_date);

        Calendar c = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss 기준");
        String formattedDate = df.format(c.getTime());

        recommend_date.setText(formattedDate);
    }

    public void getRecommendedProductList() {
        SQLiteDatabase db = DBInstance.getReadableDatabase();

        ArrayList<String> liked_keywords = new ArrayList<>();

        String SQL = "select DISTINCT " + "C.keyword_name " +
                     "from " +
                     "searched_product AS S, keyword_in_combination_local AS K, " +
                     "keyword_count_local AS C, matching_combination_local AS M " +
                     "where " + "S.like = 1 " +
                     "AND S.combination_keyword = K.combination_keyword " +
                     "AND K.combination_keyword = M.combination_keyword " +
                     "AND K.keyword_name = C.keyword_name " +
                     "order by C.count DESC";

        Cursor liked_keywords_cursor = db.rawQuery(SQL, null);

        if(liked_keywords_cursor != null &&
                liked_keywords_cursor.getCount() != 0)
        {
            liked_keywords_cursor.moveToNext();

            do {
                liked_keywords.add(liked_keywords_cursor.getString(0));
            }while(liked_keywords_cursor.moveToNext());
        }

        liked_keywords_cursor.close();
        db.close();

        try
        {
            DBInstance.getRecommendedUrls(liked_keywords, new AsyncResponse() {
                ListView listView;
                RecommendProduct_ListViewAdapter listViewAdapter;
                ArrayList<RecommendProduct_ListItem> listItems = new ArrayList<>();

                @Override
                public void processFinish(Object output) {
                    ArrayList<HashMap> outputList = (ArrayList<HashMap>) output;
                    listView = (ListView) findViewById(R.id.recommend_product_list_view);

                    int number = 1;
                    String combi_keyword;
                    String matching_url;

                    for(int i=0;i<outputList.size();i++)
                    {
                        combi_keyword = (String) outputList.get(i).get("combination_keyword");
                        matching_url = (String) outputList.get(i).get("matching_image_url");

                        boolean is_there_flag = false;
                        for(int j=0;j<listItems.size();j++) {
                            if(combi_keyword.equals(listItems.get(j).getKeyword())) {
                                is_there_flag = true;
                            }
                        }

                        if(!is_there_flag)
                        {
                            listItems.add(new RecommendProduct_ListItem(Integer.toString(number),
                                    combi_keyword, matching_url));
                            number++;
                        }
                    }

                    listViewAdapter = new RecommendProduct_ListViewAdapter(getApplicationContext(),
                                                                            listItems);
                    listView.setAdapter(listViewAdapter);
                }
            });
        }
        catch(IOException ie)
        {

        }
    }

    public void makeLocalDummyData() {
        MainActivity.DBInstance.insertSearchedProduct("nike shoes running air",
                                                      20170527, 1,
                                                       "http://img.wondershoes.co.kr/img/d_14345/1434590/IMG_L.jpg",
                                                       35000, "http://www.naver.com");
        MainActivity.DBInstance.insertSearchedProduct("Starbucks tumbler 400ml stainless",
                                                      20170605, 1,
                                                       "http://ecx.images-amazon.com/images/I/31snxgr7C0L.jpg",
                                                        56000, "http://www.naver.com");
        MainActivity.DBInstance.insertSearchedProduct("woman wallet MCM long black",
                                                      20170402, 1,
                                                       "http://ecx.images-amazon.com/images/I/417u6vjlu5L._AC_UL500_SR500,500_.jpg",
                                                        235000, "http://www.naver.com");

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

    public void makeServerDummyData()
    {
        String matching_combination_table_name = "matching_combination";
        String keyword_in_combination_table_name = "keyword_in_combination";
        String keyword_count_table_name = "keyword_count";

        String matching_params[][] = new String[10][2];

        matching_params[0][0] = "nike shoes running woman free";
        matching_params[0][1] = "http://gdimg.gmarket.co.kr/goods_image2/shop_img/975/208/975208697.jpg";

        matching_params[1][0] = "puma shoes casual woman";
        matching_params[1][1] = "http://openimage.interpark.com/goods_image/6/3/9/4/4982176394s.jpg";

        matching_params[2][0] = "woman wallet MCM red";
        matching_params[2][1] = "http://i.011st.com/ex_t/R/400x400/1/85/0/src/pd/17/8/5/2/3/3/5/fTXmq/1780852335_B.jpg";

        matching_params[3][0] = "woman wallet montblanc long";
        matching_params[3][1] = "http://by-seconds.com/img/d_13465/1346476/59104c0276ef8.jpg";

        matching_params[4][0] = "Starbucks tumbler set blue";
        matching_params[4][1] = "http://bitcdn.bit-play.com/unibox/2016/11/02/09/b364923fc504b5a32cec0699b91e52c9_1118019_450.jpg";

        matching_params[5][0] = "stainless tumbler 330ml lock";
        matching_params[5][1] = "http://image.unit808.com/data/100mc_data//images/product/22/06/11/33/32/b_2206113332.gif";

        matching_params[6][0] = "woman summer casual pants";
        matching_params[6][1] = "http://shopping.phinf.naver.net/main_1147591/11475917467.20170507164948.jpg";

        matching_params[7][0] = "woman winter casual skirt";
        matching_params[7][1] = "http://fashion.freeship.co.kr/goodsimg/9302/2017-05-12/big/32742891314.jpg";

        matching_params[8][0] = "Starbucks tumbler Christmas";
        matching_params[8][1] = "http://i.011st.com/ex_t/R/400x400/1/85/0/src/pd/17/1/3/4/5/3/1/LWqMQ/1767134531_B.jpg";

        matching_params[9][0] = "woman watch black city";
        matching_params[9][1] = "http://shopping.phinf.naver.net/main_1166994/11669942818.20170608194624.jpg";

        String keyword_combination_params[][] = new String[40][2];

        keyword_combination_params[0][1] = "nike";
        keyword_combination_params[0][0] = "nike shoes running woman free";
        keyword_combination_params[1][1] = "shoes";
        keyword_combination_params[1][0] = "nike shoes running woman free";
        keyword_combination_params[2][1] = "running";
        keyword_combination_params[2][0] = "nike shoes running woman free";
        keyword_combination_params[3][1] = "woman";
        keyword_combination_params[3][0] = "nike shoes running woman free";
        keyword_combination_params[4][1] = "free";
        keyword_combination_params[4][0] = "nike shoes running woman free";

        keyword_combination_params[5][1] = "puma";
        keyword_combination_params[5][0] = "puma shoes casual woman";
        keyword_combination_params[6][1] = "shoes";
        keyword_combination_params[6][0] = "puma shoes casual woman";
        keyword_combination_params[7][1] = "casual";
        keyword_combination_params[7][0] = "puma shoes casual woman";
        keyword_combination_params[8][1] = "woman";
        keyword_combination_params[8][0] = "puma shoes casual woman";

        keyword_combination_params[9][1] = "woman";
        keyword_combination_params[9][0] = "woman wallet MCM red";
        keyword_combination_params[10][1] = "wallet";
        keyword_combination_params[10][0] = "woman wallet MCM red";
        keyword_combination_params[11][1] = "MCM";
        keyword_combination_params[11][0] = "woman wallet MCM red";
        keyword_combination_params[12][1] = "red";
        keyword_combination_params[12][0] = "woman wallet MCM red";

        keyword_combination_params[13][1] = "woman";
        keyword_combination_params[13][0] = "woman wallet montblanc long";
        keyword_combination_params[14][1] = "wallet";
        keyword_combination_params[14][0] = "woman wallet montblanc long";
        keyword_combination_params[15][1] = "montblanc";
        keyword_combination_params[15][0] = "woman wallet montblanc long";
        keyword_combination_params[16][1] = "long";
        keyword_combination_params[16][0] = "woman wallet montblanc long";

        keyword_combination_params[17][1] = "Starbucks";
        keyword_combination_params[17][0] = "Starbucks tumbler set blue";
        keyword_combination_params[18][1] = "tumbler";
        keyword_combination_params[18][0] = "Starbucks tumbler set blue";
        keyword_combination_params[19][1] = "set";
        keyword_combination_params[19][0] = "Starbucks tumbler set blue";
        keyword_combination_params[20][1] = "blue";
        keyword_combination_params[20][0] = "Starbucks tumbler set blue";

        keyword_combination_params[21][1] = "stainless";
        keyword_combination_params[21][0] = "stainless tumbler 330ml lock";
        keyword_combination_params[22][1] = "tumbler";
        keyword_combination_params[22][0] = "stainless tumbler 330ml lock";
        keyword_combination_params[23][1] = "330ml";
        keyword_combination_params[23][0] = "stainless tumbler 330ml lock";
        keyword_combination_params[24][1] = "lock";
        keyword_combination_params[24][0] = "stainless tumbler 330ml lock";

        keyword_combination_params[25][1] = "woman";
        keyword_combination_params[25][0] = "woman summer casual pants";
        keyword_combination_params[26][1] = "summer";
        keyword_combination_params[26][0] = "woman summer casual pants";
        keyword_combination_params[27][1] = "casual";
        keyword_combination_params[27][0] = "woman summer casual pants";
        keyword_combination_params[28][1] = "pants";
        keyword_combination_params[28][0] = "woman summer casual pants";

        keyword_combination_params[29][1] = "woman";
        keyword_combination_params[29][0] = "woman winter casual skirt";
        keyword_combination_params[30][1] = "winter";
        keyword_combination_params[30][0] = "woman winter casual skirt";
        keyword_combination_params[31][1] = "casual";
        keyword_combination_params[31][0] = "woman winter casual skirt";
        keyword_combination_params[32][1] = "skirt";
        keyword_combination_params[32][0] = "woman winter casual skirt";

        keyword_combination_params[33][1] = "Starbucks";
        keyword_combination_params[33][0] = "Starbucks tumbler Christmas";
        keyword_combination_params[34][1] = "tumbler";
        keyword_combination_params[34][0] = "Starbucks tumbler Christmas";
        keyword_combination_params[35][1] = "Christmas";
        keyword_combination_params[35][0] = "Starbucks tumbler Christmas";

        keyword_combination_params[36][1] = "woman";
        keyword_combination_params[36][0] = "woman watch black city";
        keyword_combination_params[37][1] = "watch";
        keyword_combination_params[37][0] = "woman watch black city";
        keyword_combination_params[38][1] = "black";
        keyword_combination_params[38][0] = "woman watch black city";
        keyword_combination_params[39][1] = "city";
        keyword_combination_params[39][0] = "woman watch black city";

        String keyword_count_params[][] = new String[40][2];

        keyword_count_params[0][0] = "nike";
        keyword_count_params[0][1] = "1000";
        keyword_count_params[1][0] = "shoes";
        keyword_count_params[1][1] = "500";
        keyword_count_params[2][0] = "running";
        keyword_count_params[2][1] = "100";
        keyword_count_params[3][0] = "woman";
        keyword_count_params[3][1] = "20000";
        keyword_count_params[4][0] = "free";
        keyword_count_params[4][1] = "10";

        keyword_count_params[5][0] = "puma";
        keyword_count_params[5][1] = "50";
        keyword_count_params[6][0] = "casual";
        keyword_count_params[6][1] = "2000";

        keyword_count_params[7][0] = "wallet";
        keyword_count_params[7][1] = "4000";
        keyword_count_params[8][0] = "MCM";
        keyword_count_params[8][1] = "400";
        keyword_count_params[9][0] = "red";
        keyword_count_params[9][1] = "1200";

        keyword_count_params[10][0] = "montblanc";
        keyword_count_params[10][1] = "50";
        keyword_count_params[11][0] = "long";
        keyword_count_params[11][1] = "420";

        keyword_count_params[12][0] = "Starbucks";
        keyword_count_params[12][1] = "250";
        keyword_count_params[13][0] = "tumbler";
        keyword_count_params[13][1] = "700";
        keyword_count_params[14][0] = "set";
        keyword_count_params[14][1] = "132";
        keyword_count_params[15][0] = "blue";
        keyword_count_params[15][1] = "6000";

        keyword_count_params[16][0] = "stainless";
        keyword_count_params[16][1] = "3";
        keyword_count_params[17][0] = "330ml";
        keyword_count_params[17][1] = "4";
        keyword_count_params[18][0] = "lock";
        keyword_count_params[18][1] = "16";

        keyword_count_params[19][0] = "summer";
        keyword_count_params[19][1] = "777";
        keyword_count_params[20][0] = "pants";
        keyword_count_params[20][1] = "3200";

        keyword_count_params[21][0] = "winter";
        keyword_count_params[21][1] = "2300";
        keyword_count_params[22][0] = "skirt";
        keyword_count_params[22][1] = "30000";

        keyword_count_params[23][0] = "Christmas";
        keyword_count_params[23][1] = "1225";

        keyword_count_params[24][0] = "watch";
        keyword_count_params[24][1] = "17000";
        keyword_count_params[25][0] = "black";
        keyword_count_params[25][1] = "23132";
        keyword_count_params[26][0] = "city";
        keyword_count_params[26][1] = "570";

        try {

            for (int i = 0; i < 10; i++) {
                MainActivity.DBInstance.insertIntoServerTable(matching_combination_table_name,
                        matching_params[i]);
            }

            for (int i = 0; i < 40; i++) {
                MainActivity.DBInstance.insertIntoServerTable(keyword_in_combination_table_name,
                        keyword_combination_params[i]);
            }

            for (int i = 0; i < 27; i++) {
                MainActivity.DBInstance.insertIntoServerTable(keyword_count_table_name,
                        keyword_count_params[i]);
            }

        }
        catch(IOException ie) {

        }

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