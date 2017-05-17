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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class Activity_ShowVisuallySimilarImages extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCct00PWxWPoXzilFo8BrgeAKawR9OiRZQ"; // input ur key
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final int NEXT_REQUEST = 1000;

    private static final String TAG = Activity_ShowVisuallySimilarImages.class.getSimpleName();

    GridView gridview;
    GridViewAdapter gridViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visually_similar_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        Bitmap bitmap = intent.getParcelableExtra("bitmap");

        gridview = (GridView)findViewById(R.id.grid_view);

        try {
            callCloudVision(bitmap);
        }
        catch (IOException e) {
            Log.d(TAG, "Image picking failed because " + e.getMessage());
        }
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Switch text to loading
        //mImageDetails.setText(R.string.loading_message);

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, ArrayList<String>>() {
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
                            webDetection.setMaxResults(20);
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
                ArrayList<VisuallySimilar_GridItem> items = new ArrayList<>();
                final ArrayList<String> urls = result;

                new AsyncTask<Object, Void, ArrayList<VisuallySimilar_GridItem>>() {
                    @Override
                    protected ArrayList<VisuallySimilar_GridItem> doInBackground(Object... params) {
                        ArrayList<VisuallySimilar_GridItem> items = new ArrayList<>();

                        for(int i=0;i<urls.size();i++) {
                            try {
                                URL url = new URL(urls.get(i));
                                Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                items.add(new VisuallySimilar_GridItem(bitmap ,urls.get(i)));
                            } catch (IOException e) {
                                System.out.println(e);
                            }
                        }

                        return items;
                    }

                    protected void onPostExecute(ArrayList<VisuallySimilar_GridItem> items) {
                        for(int i=0;i< urls.size();i=i+2)
                        {
                            if(i+1 < urls.size()) {
                                Bitmap bitmap1 = items.get(i).getImage();
                                Bitmap bitmap2 = items.get(i+1).getImage();

                                Bitmap resizedBitmap1;
                                Bitmap resizedBitmap2;

                                int bitmap1_width = bitmap1.getWidth();
                                int bitmap1_height = bitmap1.getHeight();

                                int bitmap2_width = bitmap2.getWidth();
                                int bitmap2_height = bitmap2.getHeight();

                                int sum_width = bitmap1_width + bitmap2_width;
                                int sum_height = bitmap1_height + bitmap2_height;

                                WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
                                Display dis = wm.getDefaultDisplay();
                                Point pt = new Point();
                                dis.getSize(pt);

                                int device_width = pt.x;
                                int device_height = pt.y;

                                int higher_width;
                                int higher_height;

                                if(bitmap1_width >= bitmap2_width) {
                                    if(bitmap1_width/2 > device_width) {

                                    }
                                    else {
                                        if(bitmap1_height >= bitmap2_height) {
                                            Log.d(TAG, Integer.toString(bitmap1_width));
                                            Log.d(TAG, Integer.toString(sum_width));
                                            Log.d(TAG, Integer.toString(device_width));
                                            Log.d(TAG, Integer.toString((int)((float)bitmap1_width/(float)sum_width * (float)device_width)));

                                            resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1,
                                                    (int)((float)bitmap1_width/(float)sum_width * (float)device_width),
                                                    bitmap1_height, false);

                                            resizedBitmap2 = Bitmap.createScaledBitmap(bitmap2,
                                                    (int)((float)bitmap1_width/(float)sum_width * (float)device_width),
                                                    bitmap1_height, false);

                                            items.get(i).setImage(resizedBitmap1);
                                            items.get(i+1).setImage(resizedBitmap2);
                                        }
                                        else {
                                            resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1,
                                                    (int)((float)bitmap1_width/(float)sum_width * (float)device_width),
                                                    bitmap2_height, false);

                                            resizedBitmap2 = Bitmap.createScaledBitmap(bitmap2,
                                                    (int)((float)bitmap1_width/(float)sum_width * (float)device_width),
                                                    bitmap2_height, false);

                                            items.get(i).setImage(resizedBitmap1);
                                            items.get(i+1).setImage(resizedBitmap2);
                                        }
                                    }
                                }
                                else {
                                    if(bitmap2_width/2 > device_width) {

                                    }
                                    else {
                                        if(bitmap1_height >= bitmap2_height) {
                                            resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1,
                                                    (int)((float)bitmap1_width/(float)sum_width * (float)device_width),
                                                    bitmap1_height, false);

                                            resizedBitmap2 = Bitmap.createScaledBitmap(bitmap2,
                                                    (int)((float)bitmap1_width/(float)sum_width * (float)device_width),
                                                    bitmap1_height, false);

                                            items.get(i).setImage(resizedBitmap1);
                                            items.get(i+1).setImage(resizedBitmap2);
                                        }
                                        else {
                                            resizedBitmap1 = Bitmap.createScaledBitmap(bitmap1,
                                                    (int)((float)bitmap1_width/(float)sum_width * (float)device_width),
                                                    bitmap2_height, false);

                                            resizedBitmap2 = Bitmap.createScaledBitmap(bitmap2,
                                                    (int)((float)bitmap1_width/(float)sum_width * (float)device_width),
                                                    bitmap2_height, false);

                                            items.get(i).setImage(resizedBitmap1);
                                            items.get(i+1).setImage(resizedBitmap2);
                                        }
                                    }
                                }
                            }
                        }

                        gridViewAdapter = new GridViewAdapter(getApplicationContext(), items);
                        gridview.setAdapter(gridViewAdapter);
                    }
                }.execute();
            }
        }.execute();
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

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

    private class VisuallySimilar_GridItem {
        private Bitmap image;
        private String url;

        public VisuallySimilar_GridItem(Bitmap image, String url) {
            this.image = image;
            this.url = url;
        }

        public Bitmap getImage() {
            return this.image;
        }

        public String getUrl() {
            return this.url;
        }

        public void setImage(Bitmap image) {
            this.image = image;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

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

                    Intent intent = new Intent(getApplicationContext(), Activity_Next.class);
                    //intent.putExtra("bitmap", bitmap);
                    intent.putExtra("url", url);
                    startActivityForResult(intent, NEXT_REQUEST);
                }
            });
        }

        public void setImage(Bitmap bitmap) {
            this.image.setImageBitmap(bitmap);
        }
    }

    
}