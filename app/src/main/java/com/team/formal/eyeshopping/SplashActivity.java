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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import static com.team.formal.eyeshopping.MainActivity.DBName;

public class SplashActivity extends Activity {
    private static final String TAG = SplashActivity.class.getSimpleName();
    public static DBHelper Splash_DBInstance;
    // View Pager 객체 전역 변수들
    final ArrayList<String> keywords = new ArrayList<>();
    final ArrayList<String> urls = new ArrayList<>();
    final ArrayList<String> uris = new ArrayList<>();
    long start_time;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Splash_DBInstance = new DBHelper(this, DBName, null, 1);

        start_time = System.currentTimeMillis();

        setRecommendedProductList();
    }

    private class splashhandler implements Runnable{
        public void run() {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("keywords", keywords);
            intent.putExtra("urls", urls);
            intent.putExtra("uris", uris);

            startActivity(intent); // 로딩이 끝난후 이동할 Activity
            SplashActivity.this.finish(); // 로딩페이지 Activity Stack에서 제거
        }
    }

    public void setRecommendedProductList() {
        SQLiteDatabase db = Splash_DBInstance.getReadableDatabase();

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

        if (liked_keywords_cursor != null &&
                liked_keywords_cursor.getCount() != 0) {
            liked_keywords_cursor.moveToNext();

            do {
                liked_keywords.add(liked_keywords_cursor.getString(0));
            } while (liked_keywords_cursor.moveToNext());
        }

        liked_keywords_cursor.close();
        db.close();

        if (liked_keywords.size() != 0) {
            try {
                Splash_DBInstance.getRecommendedUrls(liked_keywords, new AsyncResponse() {
                    @Override
                    public void processFinish(Object output) {
                        ArrayList<HashMap> outputList = (ArrayList<HashMap>) output;

                        String combi_keyword;
                        String matching_url;

                        for (int i = 0; i < outputList.size(); i++) {
                            combi_keyword = (String) outputList.get(i).get("combination_keyword");
                            matching_url = (String) outputList.get(i).get("matching_image_url");

                            boolean is_there_flag = false;
                            for (int j = 0; j < keywords.size(); j++) {
                                if (combi_keyword.equals(keywords.get(j))) {
                                    is_there_flag = true;
                                }
                            }

                            if (!is_there_flag) {
                                keywords.add(combi_keyword);
                                urls.add(matching_url);
                                if (urls.size() >= 3) break;
                            }
                        }

                        new AsyncTask<Object, Void, ArrayList<Bitmap>>() {

                            @Override
                            protected ArrayList<Bitmap> doInBackground(Object... params) {
                                for (int i = 0; i < urls.size(); i++) {
                                    try {
                                        URL url = new URL(urls.get(i));
                                        String uri;
                                        Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                        File tempDir= getApplicationContext().getCacheDir();
                                        tempDir=new File(tempDir.getAbsolutePath()+"/.temp/");
                                        tempDir.mkdir();
                                        File tempFile = File.createTempFile("selected_image", ".jpg", tempDir);
                                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                                        byte[] bitmapData = bytes.toByteArray();

                                        //write the bytes in file
                                        FileOutputStream fos = new FileOutputStream(tempFile);
                                        fos.write(bitmapData);
                                        fos.flush();
                                        fos.close();
                                        uri = Uri.fromFile(tempFile).toString();
                                        uris.add(uri);
                                    } catch (IOException e) {
                                        System.out.println(e);
                                    }
                                }

                                return new ArrayList<Bitmap>();
                            }

                            @Override
                            protected void onPostExecute(ArrayList<Bitmap> bitmaps) {
                                long end_time = System.currentTimeMillis();
                                long passedtime = end_time-start_time;
                                long default_time = 1500;
                                long delay_time = default_time - passedtime;

                                if(delay_time > 0) {
                                    Handler hd = new Handler();
                                    hd.postDelayed(new splashhandler(), delay_time);
                                }
                                else {
                                    Handler hd = new Handler();
                                    hd.postDelayed(new splashhandler(), 0);
                                }

                            }
                        }.execute();
                    }
                }, false);
            } catch (IOException ie) {

            }
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
}