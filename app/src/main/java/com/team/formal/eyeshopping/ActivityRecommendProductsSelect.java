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

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

public class ActivityRecommendProductsSelect extends AppCompatActivity {
    private static final int NEXT_REQUEST = 1000;
    private static final String TAG = ActivityRecommendProductsSelect.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend_products_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Button cancleButton = (Button)findViewById(R.id.recommend_selection_cancle);
        final Button selectButton = (Button)findViewById(R.id.recommend_selection_select);
        final ImageView imageView = (ImageView)findViewById(R.id.recommend_selection_image_view);
        TextView toolbar_title = (TextView)findViewById(R.id.recommend_title);

        // Url과 Uri를 부모 액티비티에서 받는다.
        Intent intent = getIntent();
        final String s_url = intent.getStringExtra("url");
        String combination_keyword = intent.getStringExtra("keyword");

        toolbar_title.setText(combination_keyword);

        // get Bitmap image from uri
        new AsyncTask<Object, Void, Void>() {
            final ProgressDialog asyncDialog = new ProgressDialog(ActivityRecommendProductsSelect.this);

            Bitmap bitmap;
            String uri;

            @Override
            protected void onPreExecute() {
                asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                asyncDialog.setMessage("Downloading the Image");

                // show dialog
                asyncDialog.show();
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Object... params) {
                try {
                    URL url = new URL(s_url);
                    bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                    File tempDir = getApplicationContext().getCacheDir();
                    tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
                    tempDir.mkdir();
                    File tempFile = File.createTempFile("selected_image", ".jpg", tempDir);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    byte[] bitmapData = bytes.toByteArray();

                    Log.i(TAG, "bitmap created");

                    //write the bytes in file
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    fos.write(bitmapData);
                    fos.flush();
                    fos.close();
                    uri = Uri.fromFile(tempFile).toString();
                } catch (IOException e) {
                    System.out.println(e);
                }

                Void vo = null;

                return (Void) vo;
            }

            protected void onPostExecute(Void vo) {
                imageView.setImageBitmap(bitmap);
                Log.d(TAG, "bitmap set");

                cancleButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

                selectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getApplicationContext(), ActivityFindingResults.class);
                        intent.putExtra("url", s_url); //  웹 url
                        intent.putExtra("uri", uri.toString()); // local uri
                        startActivityForResult(intent, NEXT_REQUEST);
                    }
                });

                asyncDialog.dismiss();
            }
        }.execute();
    }
}