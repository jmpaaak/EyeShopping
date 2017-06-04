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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.WebDetection;
import com.google.api.services.vision.v1.model.WebImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class ActivityShowVisuallySimilarImages extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCct00PWxWPoXzilFo8BrgeAKawR9OiRZQ";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int SELECT_REQUEST = 1000;
    private static final String TAG = ActivityShowVisuallySimilarImages.class.getSimpleName();

    // Grid View 전역 변수
    GridView gridview;
    GridViewAdapter gridViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visually_similar_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();
        Uri uri = Uri.parse(intent.getStringExtra("uri"));
        Bitmap bitmap = null;
        // get Bitmap image from uri
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        gridview = (GridView)findViewById(R.id.grid_view);

        try {
            callCloudVision(bitmap);
        }
        catch (IOException e) {
            Log.d(TAG, "Image picking failed because " + e.getMessage());
        }
    }

    /*
        Child Activity에서 종료시 호출 되는 함수
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_REQUEST) {

        }
    }

    /*
        비젼 API 호출 후, Image Url을 저장하고 이를 그리드 뷰에 담는다.
        총 2개의 비동기
     */
    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, ArrayList<String>>() {
            final ProgressDialog asyncDialog = new ProgressDialog(ActivityShowVisuallySimilarImages.this);

            @Override
            protected void onPreExecute() {
                asyncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                asyncDialog.setMax(20);
                asyncDialog.setMessage("Downloading Visually Similar Images..");

                // show dialog
                asyncDialog.show();
                super.onPreExecute();
            }

            @Override
            protected ArrayList<String> doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);
                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature webDetection = new Feature();
                            webDetection.setType("WEB_DETECTION");
                            webDetection.setMaxResults(3);
                            add(webDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();

                    return convertResponseToString(response);
                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                }
                return null;
            }

            protected void onPostExecute(ArrayList<String> result) {
                final ArrayList<String> urls = result;
                asyncDialog.setMax(urls.size());

                new AsyncTask<Object, Void, ArrayList<VisuallySimilar_GridItem>>() {
                    @Override
                    protected ArrayList<VisuallySimilar_GridItem> doInBackground(Object... params) {
                        ArrayList<VisuallySimilar_GridItem> items = new ArrayList<>();

                        for(int i=0;i<urls.size();i++) {
                            try {
                                URL url = new URL(urls.get(i));
                                String uri;
                                Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());

                                File tempDir= getApplicationContext().getFilesDir();
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

                                items.add(new VisuallySimilar_GridItem(bitmap ,urls.get(i),uri));

                                asyncDialog.setProgress(i+1);
                            } catch (IOException e) {
                                System.out.println(e);
                            }
                        }

                        return items;
                    }

                    protected void onPostExecute(ArrayList<VisuallySimilar_GridItem> items) {
                        gridViewAdapter = new GridViewAdapter(getApplicationContext(), items);
                        gridview.setAdapter(gridViewAdapter);

                        asyncDialog.dismiss();
                    }
                }.execute();
            }
        }.execute();
    }

    /*
        비동기 태스크 후 호출 되는 함수
     */
    private ArrayList<String> convertResponseToString(BatchAnnotateImagesResponse response) {

        ArrayList<String> urls = new ArrayList<>();

        String url = "";

        WebDetection annotation = response.getResponses().get(0).getWebDetection();
        if (annotation != null) {
            for (WebImage image : annotation.getVisuallySimilarImages()) {
                url = String.format(Locale.KOREAN, "%s", image.getUrl());
                urls.add(url);
            }
        }

        return urls;
    }

    /*
        그리드뷰 어댑터, 그리드 뷰를 inflate하여 객체화 한다
     */
    private class GridViewAdapter extends BaseAdapter {
        Context context;
        ArrayList<VisuallySimilar_GridItem> gridItems;

        private GridViewAdapter(Context context, ArrayList<VisuallySimilar_GridItem> gridItems) {
            this.context = context;
            this.gridItems = gridItems;
        }

        public int getCount() {
            return gridItems.size();
        }

        public VisuallySimilar_GridItem getItem(int position) {
            return gridItems.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            VisuallySimilar_GridView gridView;

            if(convertView == null) {
                gridView = new VisuallySimilar_GridView(this.context, this.gridItems.get(position));
            }else {
                gridView = (VisuallySimilar_GridView) convertView;
            }

            return gridView;
        }
    }

    /*
        그리드뷰 아이템 그리드 뷰에 들어갈 정보를 담고 있다
     */
    private class VisuallySimilar_GridItem {
        private Bitmap image;
        private String url;
        private String uri;

        public VisuallySimilar_GridItem(Bitmap image, String url, String uri) {
            this.image = image;
            this.url = url;
            this.uri = uri;
        }

        public Bitmap getImage() {
            return this.image;
        }

        public String getUrl() {
            return this.url;
        }

        public String getUri() { return this.uri; }

        public void setImage(Bitmap image) {
            this.image = image;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setUri(String uri) { this.uri = uri; }

    }

    /*
        그리드뷰 뷰, xml과 연결된 레이아웃 클래스
     */
    private class VisuallySimilar_GridView extends LinearLayout {

        private ImageView image;

        public VisuallySimilar_GridView(Context context, final VisuallySimilar_GridItem aItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.grid_view_item, this, true);

            image = (ImageView)findViewById(R.id.grid_item_image_view);
            image.setImageBitmap(aItem.getImage());

            image.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap bitmap = aItem.getImage();
                    String url = aItem.getUrl();
                    String uri = aItem.getUri();

                    Intent intent = new Intent(getApplicationContext(), ActivityShowVisuallySimilarImagesSelect.class);
                    intent.putExtra("url", url);
                    intent.putExtra("uri", uri);
                    startActivityForResult(intent, SELECT_REQUEST);
                }
            });
        }

        public void setImage(Bitmap bitmap) {
            this.image.setImageBitmap(bitmap);
        }
    }
}