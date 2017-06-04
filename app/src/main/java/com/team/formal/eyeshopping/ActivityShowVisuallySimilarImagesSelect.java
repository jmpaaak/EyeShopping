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

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ActivityShowVisuallySimilarImagesSelect extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCct00PWxWPoXzilFo8BrgeAKawR9OiRZQ"; // input ur key
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final int NEXT_REQUEST = 1000;
    private static final String TAG = ActivityShowVisuallySimilarImagesSelect.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visually_similar_image_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Button cancleButton = (Button)findViewById(R.id.selection_cancle);
        Button selectButton = (Button)findViewById(R.id.selection_select);

        // Url과 Uri를 부모 액티비티에서 받는다.
        Intent intent = getIntent();
        final String url = intent.getStringExtra("url");
        final Uri uri = Uri.parse(intent.getStringExtra("uri"));

        // get Bitmap image from uri
        try {
            // 비트맵을 내부 저장소에서 Uri를 활용해 얻는다
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            // 이미지 뷰에 띄운다
            ImageView imageView = (ImageView)findViewById(R.id.selection_image_view);
            imageView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

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
                intent.putExtra("url", url); //  웹 url
                intent.putExtra("uri", uri.toString()); // local uri
                startActivityForResult(intent, NEXT_REQUEST);
            }
        });
    }
}