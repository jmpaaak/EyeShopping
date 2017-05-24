package com.google.sample.cloudvision;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

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
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Administrator on 2017-05-24.
 */

public class Activity_Search_PagesWithMatching extends Activity{
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCct00PWxWPoXzilFo8BrgeAKawR9OiRZQ"; // input ur key
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final String TAG = Activity_Search_PagesWithMatching.class.getSimpleName();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_pageswithmatching);

        ProgressDialog dialog = ProgressDialog.show(Activity_Search_PagesWithMatching.this, "",
                "로딩 중입니다. 잠시 기다려주세요", true);


        Intent t = getIntent();
        String uri = t.getStringExtra("uri");
        String url= t.getStringExtra("url");

        Log.d("SP", url);
        Log.d("SP", uri);


    }
    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, ArrayList<String>>() {
            final ProgressDialog asyncDialog = new ProgressDialog(Activity_Search_PagesWithMatching.this);

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


        };
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
}