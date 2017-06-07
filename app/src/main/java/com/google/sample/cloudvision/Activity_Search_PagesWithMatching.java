package com.google.sample.cloudvision;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.JetPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

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
import com.google.api.services.vision.v1.model.WebPage;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by Administrator on 2017-05-24.
 */

public class Activity_Search_PagesWithMatching extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCct00PWxWPoXzilFo8BrgeAKawR9OiRZQ"; // input ur key
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final String clientID = "vI5pJhlnafXVKt13Z5mF";
    private static final String clientSecret = "S60E47pO0M";

    private static final String TAG = Activity_Search_PagesWithMatching.class.getSimpleName();

    private static final int SEARCH_REQUEST = 1000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search_pageswithmatching);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        Uri uri = Uri.parse(intent.getStringExtra("uri"));

        TextView textView = (TextView) findViewById(R.id.text_view);
        String text = "Url : " + url + "\nUri: " + uri.toString();
        textView.setText(text);


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

        try {
             callCloudVision(bitmap);

        } catch (IOException e) {
            Log.d(TAG, "Image picking failed because " + e.getMessage());
        }

    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, ArrayList<String>>() {

            @Override
            protected void onPreExecute() {

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

            @Override
            protected void onPostExecute(ArrayList<String> urls) {
                super.onPostExecute(urls);
                //  매개변수는 urls .

                for(int i=0; i<urls.size(); i++){
                    Log.d("pages", urls.get(i));
                }
                ArrayList<String[]> test1 = urlParsing(urls);
                ArrayList<String[]> parsedUrl = urlParsing(urls);
                Map<String, Integer> map = new HashMap<String, Integer>();

                // 1. url���� �ǵ� /�� ©���� �װ͵��� _-
                for (int i = 0; i < parsedUrl.size(); i++) {
                    for (int j = 0; j < parsedUrl.get(i).length; j++) {
                        System.out.println(parsedUrl.get(i)[j]);
                        Integer count = map.get(parsedUrl.get(i)[j]);
                        map.put(parsedUrl.get(i)[j], (count == null) ? 1 : count + 1);
                    }
                    System.out.println("");
                }

                // �ߺ� �Ȱ� ������, ã���ְ� �ߺ��� keyword�� ���� ���¿��� �������� �̾��༭
                // Ű���带 �����Ѵ�.
                // �׸��� �װ͵��� naver Shop api�� �˻���
                ArrayList<String> rankCount = new ArrayList<>();
                ArrayList<ArrayList<String>> resultArr = new ArrayList<ArrayList<String>>();

                for (Map.Entry<String, Integer> entry : map.entrySet()) {
                    if (entry.getValue() >= 3) {
                        System.out.println("keyword : " + entry.getKey() + " Count : " + entry.getValue());
                        rankCount.add(entry.getKey());
                    }
                }

                for (String arr[] : parsedUrl) {
                    int count = 0;
                    boolean check[] = new boolean[arr.length];
                    ArrayList<String> strArr = new ArrayList<>();

                    for (int i = 0; i < arr.length; i++) {
                        for (String rank : rankCount) {
                            if (arr[i].equals(rank)) {
                                check[i] = true;
                                strArr.add(arr[i]);
                                count++;
                            }
                        }
                        Log.d("strArr",strArr.toString());
                    }

                    int rand;
                    int randSize = randomRange(1, arr.length - count);
                    for (int i = 0; i < randSize; i++) {
                        while (check[(rand = randomRange(0, arr.length - 1))]) {
                            break;
                        }
                        strArr.add(arr[rand]);
                        check[rand] = true;
                    }
                    resultArr.add(strArr);
                    Log.d("raa",resultArr.toString());
                }
                List<Shop> parsingResult =null;
                for(int i=0; i<resultArr.size(); i++){
                    System.out.println(resultArr.get(i).toString().replaceAll(", ", "%20"));
                    Log.d("uri",resultArr.get(i).toString().replaceAll(", ", "%20"));
                    String shopResult= null;
                    try {
                        shopResult = naverShopApi(resultArr.get(i).toString().replaceAll(", ", "%20"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        parsingResult = parsingShopResultXml(shopResult);
                        for (Shop shop : parsingResult){
                            Log.d("asd", shop.getImage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }.execute();
    }


        private ArrayList<String> convertResponseToString(BatchAnnotateImagesResponse response) {

            ArrayList<String> urls = new ArrayList<>();

            String url = "";

            WebDetection annotation = response.getResponses().get(0).getWebDetection();
            if (annotation != null) {
                for (WebPage page : annotation.getPagesWithMatchingImages()) {
                    url = String.format(Locale.KOREAN, "%s", page.getUrl());
                    urls.add(url);
                }
            }
            return urls;
        }



    public ArrayList<String[]> urlParsing(ArrayList<String> urls) {
        ArrayList<Integer> lastIndex = new ArrayList<Integer>();
        ArrayList<String> tokenList = new ArrayList<String>();
        ArrayList<String[]> keywordList = new ArrayList<String[]>();

        // 맨뒤에 "/" 짜르는 작업
        for (int i = 0; i < urls.size(); i++) {
            lastIndex.add(urls.get(i).lastIndexOf("/"));
        }

        for (int i = 0; i < urls.size(); i++) {
            tokenList.add(urls.get(i).substring(lastIndex.get(i) + 1, urls.get(i).length()));
            // System.out.println(tokenList);
        }
        // 짤라진 문장에서 _-로 파싱...
        for (int i = 0; i < tokenList.size(); i++) {
            keywordList.add(tokenList.get(i).split("[-_]"));
        }

        return keywordList;
    }

    // 네이버 쇼핑 api 검색
    public String naverShopApi(final String string) throws IOException, ExecutionException, InterruptedException {

        String res =  new AsyncTask<Object, Void, String>() {

            @Override
            protected String doInBackground(Object[] params) {

                URL url = null;
                try {
                    url = new URL("https://openapi.naver.com/v1/search/shop.xml?query=" + string + "&display=50");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                HttpURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("X-Naver-Client-ID", clientID);
                    urlConnection.setRequestProperty("X-Naver-Client-Secret", clientSecret);
                    urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
                    urlConnection.setRequestProperty("Accept","*/*");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                InputStream in = null;
                try {
                    in = new BufferedInputStream(urlConnection.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String data = "";
                String msg = null;

                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                try {
                    while ((msg = br.readLine()) != null) {
                        data += msg;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.i("msg of br: ", data);

                return data;

            }

            @Override
            protected void onPostExecute(String s) {
            }
        }.execute().get();

        return res;
    }

    public static int randomRange(int n1, int n2) {
        return (int) (Math.random() * (n2 - n1 + 1)) + n1;
    }

    // xml 형태의 결과를 파싱하는 작업
    public List<Shop> parsingShopResultXml(String data) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser(); // �����ϴ°� ���

        List<Shop> list = null ;
        parser.setInput(new StringReader(data));
        int eventType = parser.getEventType();
        Shop b = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    list = new ArrayList<Shop>();
                    break;
                case XmlPullParser.END_DOCUMENT:// ������ ��
                    break;
                case XmlPullParser.START_TAG: { // ������ �����ϸ� ����
                    String tag = parser.getName();
                    switch (tag) {
                        case "item": // item�� ���ȴٴ°��� ���ο� å�� ���´ٴ°�
                            b = new Shop();
                            break;
                        case "title":
                            if (b != null)
                                b.setTitle(parser.nextText());
                            break;
                        case "link":
                            if (b != null)
                                b.setLink(parser.nextText());
                            break;
                        case "image":
                            if (b != null)
                                b.setImage(parser.nextText());
                            break;
                        case "total":
                            if (b != null)
                                b.setTotal(parser.next());
                            break;
                        case "lprice":
                            if (b != null)
                                b.setLprice(parser.next());
                            break;
                        case "hprice":
                            if (b != null)
                                b.setHprice(parser.next());
                            break;
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    String tag = parser.getName();
                    if (tag.equals("item")) {
                        list.add(b);
                        b = null;
                    }
                }
            }
            eventType = parser.next();
        }
        return list;
    }

}