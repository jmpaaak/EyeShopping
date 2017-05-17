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

package com.google.sample.cloudvision;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;

public class Activity_Next extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCct00PWxWPoXzilFo8BrgeAKawR9OiRZQ"; // input ur key
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final int NEXT_REQUEST = 1000;

    private static final String TAG = Activity_Next.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        //Bitmap bitmap = intent.getParcelableExtra("bitmap");
        final String intent_url = intent.getStringExtra("url");

        new AsyncTask<Object, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object... params) {
                Bitmap bitmap = null;

                try {
                    URL url = new URL(intent_url);
                    bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                } catch (IOException e) {
                        System.out.println(e);
                }

                return bitmap;
            }

            protected void onPostExecute(Bitmap bitmap) {
                TextView textView = (TextView)findViewById(R.id.text_view);
                Log.d(TAG, "Url :" + intent_url);
                String text = "Url : " + intent_url;
                textView.setText(text);

                ImageView imageView = (ImageView)findViewById(R.id.image_view);
                imageView.setImageBitmap(bitmap);
            }
        }.execute();
    }
}