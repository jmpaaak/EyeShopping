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
            final ProgressDialog asyncDialog = new ProgressDialog(Activity_Search_PagesWithMatching.this);

            @Override
            protected void onPreExecute() {
                asyncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                asyncDialog.setMax(20);
                asyncDialog.setMessage("Search_pagesWithMatching...");

                // show dialog
                //asyncDialog.show();
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

                ArrayList<String[]> test1 = urlParsing(urls);
                HashMap<Integer, ArrayList<String>> hash = new HashMap<Integer, ArrayList<String>>();
                ArrayList<SearchNumStringSet> resultArr = new ArrayList<SearchNumStringSet>();

                for (int i = 0; i < test1.size(); i++) {
                    String[] strArr = test1.get(i);
                    String firstStr = strArr[0];

                    boolean searchResult = false;

                    String checkFirstResult = null;
                    try {

                        checkFirstResult = naverShopApi(firstStr);
                        Log.d("cfr__",checkFirstResult);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                 /*   if (checkFirstResult.contains("<display>0</display>")) {

                        searchResult = false;

                    }*/

                    // 첫번째 검색어 검색 (firstArr)

                    if (searchResult) {
                        int rand, strLength = strArr.length;
                        boolean check[] = new boolean[strLength + 1];
                        int oddsSize = strLength / 2;
                        int evenSize = strLength / 2;

                        if (strLength % 2 == 1)
                            oddsSize++;

                        String odds[] = new String[oddsSize];
                        String even[] = new String[evenSize];

                        int oddsCount = 0, evenCount = 0;

                        for (int j = 0; j < strLength; j++) {
                            while (check[(rand = randomRange(0, strLength - 1))]) {
                            }

                            if (j % 2 == 1 || j == (strLength - 1)) {
                                odds[oddsCount++] = strArr[rand];
                            } else {
                                even[evenCount++] = strArr[rand];
                            }

                            check[rand] = true;
                        }

                        String oddsStr = "", evenStr = "";
                        System.out.println("O : ");
                        for (String o : odds) {
                            System.out.println(o);
                            // 마지막엔,ㄴ %20이 들어가면 안되므로
                            if (!o.equals(odds[odds.length - 1]))
                                oddsStr = oddsStr + o + "%20";
                            else
                                oddsStr += o;
                        }

                        String checkResult = null;
                        try {
                            checkResult = naverShopApi(oddsStr);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        List<Shop> sh = new ArrayList<Shop>();
                        try {
                            sh = parsingShopResultXml(checkResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        inputData(hash, sh.size(), oddsStr);

                        System.out.println();

                        System.out.println("E : ");
                        for (String e : even) {

                            System.out.println(e);
                            if (!e.equals(even[even.length - 1]))
                                evenStr = evenStr + e + "%20";
                            else
                                evenStr += e;
                        }
                        try {
                            checkResult = naverShopApi(evenStr);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            sh = parsingShopResultXml(checkResult);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        inputData(hash, sh.size(), evenStr);
                        // sh.size가 검색 결과 숫자..
                        System.out.println();

                    }
                }
                // key 값만 다 모아오는 것.
                Iterator itr = hash.keySet().iterator();
                int keyArr[] = new int[hash.size()];
                int sizeCount = 0;

                while (itr.hasNext()) {
                    int key = (int) itr.next();
                    keyArr[sizeCount++] = key;
                }

                // 키 값만 정렬
                Arrays.sort(keyArr);

                for (int i = 0; i < hash.size(); i++) {
                    int key = keyArr[i];
                    ArrayList<String> strArr = hash.get(key);

                    for (String str : strArr) {
                        resultArr.add(new SearchNumStringSet(key, str));
                        System.out.println(key + " / " + str);
                    }

                }
                for(int i=0; i<resultArr.size();i++){
                    Log.d("keyword",resultArr.get(i).searchStr);
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
                } finally {
                    urlConnection.disconnect();
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

    // 각 url의 length에 따라 랜덤.
    public static int randomRange(int n1, int n2) {
        return (int) (Math.random() * (n2 - n1 + 1)) + n1;
    }

    // 클래스를 만들어  키워드와 그에 해당하는 결과값 갯수 생성자 구축.
    class SearchNumStringSet {
        private int searchNum;
        private String searchStr;

        public SearchNumStringSet(int searchNum, String searchStr) {
            this.searchNum = searchNum;
            this.searchStr = searchStr;
        }

        public int getSearchNum() {
            return searchNum;
        }

        public void setSearchNum(int searchNum) {
            this.searchNum = searchNum;
        }

        public String getSearchStr() {
            return searchStr;
        }

        public void setSearchStr(String searchStr) {
            this.searchStr = searchStr;
        }
    }

    public static void inputData(HashMap<Integer, ArrayList<String>> hash, int num, String str) {

        // 검색결과가 없으면 넣지 않는다
        if (num == 0)
            return;

        ArrayList<String> arrStr = hash.get(num);
        // 검색 결과 숫자가 이미 해쉬에 있을 경우

        if (arrStr != null) {
            arrStr.add(str);
            hash.put(num, arrStr);
        } else {
            // 검색결과 숫자가 키 값에 들어 있지 않은 경우
            ArrayList<String> arr = new ArrayList<String>();
            arr.add(str);
            hash.put(num, arr);
        }
    }


}