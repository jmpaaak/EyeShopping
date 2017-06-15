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
import android.database.Cursor;
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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActivityFindingResults extends AppCompatActivity {

    private static final String CLOUD_VISION_API_KEY = "AIzaSyCct00PWxWPoXzilFo8BrgeAKawR9OiRZQ"; // input ur key
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final String clientID = "vI5pJhlnafXVKt13Z5mF";
    private static final String clientSecret = "S60E47pO0M";
    private static final String TAG = ActivityFindingResults.class.getSimpleName();

    private Bitmap mNaverPrImg;
    private Mat userSelImg = null;

    // Grid View 전역 변수
    GridView gridView;
    GridViewAdapter gridViewAdapter;

    ArrayList<Results_GridItem> findingItems = new ArrayList<>();

    static {
        System.loadLibrary("native-lib");
    }

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);

    public native void CornerHarrisDemo(long addrInputImage, long addrOutput);

    public native int AkazeFeatureMatching(long userSelImage, long naverPrImage);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finding_results);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        gridView = (GridView) findViewById(R.id.list_view);

        try {
            // TODO 이것도 naverPrImgTarget 처럼.. url로 처리
            userSelImg = Utils.loadResource(this, R.drawable.user_image, CvType.CV_8UC4); // return BGR 순
            // naverPrImg = Utils.loadResource(this, R.drawable.marmont_bag, CvType.CV_8UC4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // made by jaeman
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        Uri uri = Uri.parse(intent.getStringExtra("uri"));

//        TextView textView = (TextView) findViewById(R.id.text_view);
//        String text = "Url : " + url + "\nUri: " + uri.toString();
//        textView.setText(text);

        Bitmap bitmap = null;
        // get Bitmap image from uri
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            callCloudVision(bitmap);
        } catch (IOException e) {
            Log.d(TAG, "Image picking failed because " + e.getMessage());
        }
        // end of jaeman

        TextView t = (TextView) findViewById(R.id.loadingText);
        t.setVisibility(View.VISIBLE);
        GridView g = (GridView) findViewById(R.id.list_view);
        g.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResume :: OpenCV library found inside package. Using it!");
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public Bitmap getBitmapFromURL(String src) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(src);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            BitmapFactory.Options op = new BitmapFactory.Options();
            op.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap myBitmap = BitmapFactory.decodeStream(input, null, op);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /*
        그리드뷰 어댑터, 그리드 뷰를 inflate하여 객체화 한다
     */
    private class GridViewAdapter extends BaseAdapter {
        Context context;
        ArrayList<ActivityFindingResults.Results_GridItem> gridItems;

        private GridViewAdapter(Context context, ArrayList<ActivityFindingResults.Results_GridItem> gridItems) {
            this.context = context;
            this.gridItems = gridItems;
        }

        public int getCount() {
            return gridItems.size();
        }

        public ActivityFindingResults.Results_GridItem getItem(int position) {
            return gridItems.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ActivityFindingResults.Results_GridView gridView;

            if(convertView == null) {
                gridView = new ActivityFindingResults.Results_GridView(this.context, this.gridItems.get(position));
            } else {
                gridView = (ActivityFindingResults.Results_GridView) convertView;
            }

            return gridView;
        }
    }

    /*
        그리드뷰 아이템 그리드 뷰에 들어갈 정보를 담고 있다
     */
    class Results_GridItem{
        private Bitmap thumb;
        private String productName;
        private String price;
        private int int_price;
        private String url;
        private ArrayList<String> keywords;
        private String combinationKeyword;
        private String thumbUrl;

        public Results_GridItem(String productName, Bitmap thumb, String price, int int_price, String url,
                                ArrayList<String> keywords, String combinationKeyword, String thumbUrl) {
            this.thumb = thumb;
            this.productName = productName;
            this.price = price;
            this.int_price = int_price;
            this.url = url;
            this.keywords = keywords;
            this.combinationKeyword = combinationKeyword;
            this.thumbUrl = thumbUrl;
        }

        public int getPrice() { return this.int_price; }
        public String getPriceText() { return this.price; }
        public Bitmap getThumb() { return this.thumb; }
        public String getProductName() { return this.productName; }
        public String getUrl() { return this.url; }
        public ArrayList<String> getKeywords() { return this.keywords; }
        public String getCombinationKeyword() { return this.combinationKeyword; }
        public String getThumbUrl() { return this.thumbUrl; }
//        public String keyworkd
    }

    /*
        그리드뷰 뷰, xml과 연결된 레이아웃 클래스
     */
    class Results_GridView extends LinearLayout {
        private ViewGroup vGroup;
        private ImageView thumbView;
        private TextView productNameView;
        private TextView priceView;

        public Results_GridView(Context context, final ActivityFindingResults.Results_GridItem aItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.grid_list_item, this, true);

            vGroup = (ViewGroup) findViewById(R.id.grid_item_frame_layout2);
            thumbView = (ImageView) findViewById(R.id.product_thumbnail);
            productNameView = (TextView) findViewById(R.id.product_name);
            priceView = (TextView) findViewById(R.id.product_price);

            thumbView.setImageBitmap(aItem.getThumb());
            productNameView.setText(aItem.getProductName());
            priceView.setText(aItem.getPriceText());

            vGroup.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ActivityFindingsResultsSelect.class);
                    //searchDate를 ActivityFindingsResultsSelect로 넘겨줌

                    ViewGroup vg = (ViewGroup) v;
                    thumbView = (ImageView) vg.findViewById(R.id.product_thumbnail);
                    productNameView = (TextView) vg.findViewById(R.id.product_name);
                    priceView = (TextView) vg.findViewById(R.id.product_price);

                    intent.putExtra("product_thumbnail",  aItem.getThumb());
                    intent.putExtra("product_name", aItem.getProductName());
                    intent.putExtra("product_price", aItem.getPriceText());
                    intent.putExtra("product_url", aItem.getUrl());

                    // insert data to DB local & server
                    insertAllAboutProduct(aItem);

                    startActivityForResult(intent, 17777);
                }
            });
        }
    }

    // made by jaeman
    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, ArrayList<String>>() {
            final ProgressDialog asyncDialog = new ProgressDialog(ActivityFindingResults.this);

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                asyncDialog.setMessage("Loading Products ...");
                asyncDialog.show();
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

                for (int i = 0; i < urls.size(); i++) {
                    Log.d("pages", urls.get(i));
                }
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
                        Log.d("strArr", strArr.toString());
                    }

                    int rand;
                    int randSize = randomRange(1, arr.length - count);
                    for (int i = 0; i < randSize; i++) {
                        while (check[(rand = randomRange(0, arr.length - 1))]) {
                        }
                        strArr.add(arr[rand]);
                        check[rand] = true;
                    }
                    resultArr.add(strArr);
                    Log.d("raa", resultArr.toString());
                }

                final ArrayList<ArrayList<String>> resultArrThread = resultArr;
                new AsyncTask<Object, Void, List<Shop>>() {
                    @Override
                    protected List<Shop> doInBackground(Object... params) {
                        List<Shop> results = new ArrayList<>();

                        if(results.size() > 5)
                            results = results.subList(0, 5);

                        for (int i = 0; i < resultArrThread.size(); i++) {
                            System.out.println(resultArrThread.get(i).toString().replaceAll(", ", "%20"));
                            Log.d("uri", resultArrThread.get(i).toString().replaceAll(", ", "%20"));

                            final String xmlRaw = resultArrThread.get(i).toString().replaceAll(", ", "%20");

                            // 1
                            URL url = null;
                            try {
                                url = new URL("https://openapi.naver.com/v1/search/shop.xml?query=" + xmlRaw + "&display=50");
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }

                            HttpURLConnection urlConnection = null;
                            try {
                                urlConnection = (HttpURLConnection) url.openConnection();
                                urlConnection.setRequestProperty("X-Naver-Client-ID", clientID);
                                urlConnection.setRequestProperty("X-Naver-Client-Secret", clientSecret);
                                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
                                urlConnection.setRequestProperty("Accept", "*/*");
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
                                if(in != null) {
                                    br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            try {
                                if(br != null) {
                                    while ((msg = br.readLine()) != null) {
                                        data += msg;
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Log.i("msg of br: ", data);

                            // 2
                            String shopResult = data;
                            try {
                                List<Shop> parsingResult = parsingShopResultXml(shopResult);
                                if(parsingResult.size() > 5)
                                    parsingResult = parsingResult.subList(0, 5);

                                for (final Shop shop : parsingResult) {
                                    Bitmap thumbImg = getBitmapFromURL(shop.getImage());
                                    if(thumbImg != null) {

                                        ArrayList<String> keywords = new ArrayList<>(
                                                Arrays.asList(resultArrThread.get(i).toString()
                                                        .replace("[", "")
                                                        .replace("]", "")
                                                        .split(","))
                                        );
                                        String combinationKeyword = resultArrThread.get(i).toString()
                                                .replace("[", "")
                                                .replace("]", "")
                                                .replaceAll(", ", " ");

                                        shop.setThumbBmp(thumbImg); // 입력 이미지 Url
                                        shop.setCombinationKeyword(combinationKeyword);
                                        shop.setKeywords(keywords);
                                        results.add(shop);
                                    }
                                }

                                if(results.size() > 10) // must be
                                    results = results.subList(0, 10);

                                for (Shop dummyShop : results) {
                                    mNaverPrImg = dummyShop.getThumbBmp();
                                    Mat userSelImgTarget = new Mat(userSelImg.width(), userSelImg.height(), CvType.CV_8UC4);
                                    Mat naverPrImgTarget = new Mat(mNaverPrImg.getWidth(), mNaverPrImg.getHeight(), CvType.CV_8UC4);

                                    Utils.bitmapToMat(mNaverPrImg, naverPrImgTarget);
                                    Imgproc.cvtColor(userSelImg, userSelImgTarget, Imgproc.COLOR_BGR2RGB);
                                    Imgproc.cvtColor(naverPrImgTarget, naverPrImgTarget, Imgproc.COLOR_RGBA2RGB);


                                    int ret = AkazeFeatureMatching(userSelImg.getNativeObjAddr(),
                                            naverPrImgTarget.getNativeObjAddr());

                                    if (ret == 1) { // find one!
                                        DecimalFormat df = new DecimalFormat("#,###");
                                        String num = df.format(dummyShop.getLprice());
                                        int exist_flag = 0;
                                        for(int ii=0;ii<findingItems.size();ii++) {
                                            if(findingItems.get(ii).getProductName() == dummyShop.getTitle()) {
                                                exist_flag = 1;
                                                break;
                                            }
                                        }
                                        if(exist_flag == 0) {
                                            findingItems.add(new Results_GridItem(dummyShop.getTitle(),
                                                    mNaverPrImg,
                                                    "최저가 " + num + "원", dummyShop.getLprice(),
                                                    dummyShop.getLink(),
                                                    dummyShop.getKeywords(),
                                                    dummyShop.getCombinationKeyword(),
                                                    dummyShop.getImage()));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } // end of for

                        return results;
                    } // end of doinbackground

                    @Override
                    protected void onPostExecute(List<Shop> shops) {
                        super.onPostExecute(shops);

                        TextView t = (TextView) findViewById(R.id.loadingText);
                        t.setVisibility(View.GONE);
                        GridView g = (GridView) findViewById(R.id.list_view);
                        g.setVisibility(View.VISIBLE);

                        if(findingItems.size() == 0) {
                            TextView tLoad = (TextView) findViewById(R.id.loadingText);
                            tLoad.setText("현재 검색 결과가 없습니다.");
                            tLoad.setVisibility(View.VISIBLE);
                            gridView.setVisibility(View.GONE);
                        } else {
                            Log.d(TAG, "finding Size!!!!" + Integer.toString(findingItems.size()));
                            Collections.sort(findingItems, new Comparator<Results_GridItem>() {
                                @Override
                                public int compare(Results_GridItem o1, Results_GridItem o2) {
                                    if(o1.getPrice() > o2.getPrice())
                                    {
                                        return 1;
                                    }
                                    else if(o1.getPrice() < o2.getPrice())
                                    {
                                        return -1;
                                    }
                                    else {
                                        return 0;
                                    }
                                }
                            });
                            for(int i=0;i<findingItems.size();i++) {
                                Log.d(TAG, "List !! " + Integer.toString(findingItems.get(i).getPrice()));
                            }
                            Log.d(TAG, "finding Size!!!!" + Integer.toString(findingItems.size()));
                            gridViewAdapter = new GridViewAdapter(getApplicationContext(), findingItems);
                            gridView.setAdapter(gridViewAdapter);
                        }

                        asyncDialog.dismiss();
                    }
                }.execute();

            } // end of PostExcute
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
            keywordList.add(tokenList.get(i)
                    .replace(".html","")
                    .replace(".php","")
                    .split("[-_]"));
        }

        return keywordList;
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
                            if (b != null) {
                                b.setTitle(RemoveHTMLTag(parser.nextText()));
                            }
                            break;
                        case "link":
                            if (b != null)
                                b.setLink(parser.nextText());
                            break;
                        case "image":
                            if (b != null)
                                b.setImage(parser.nextText()+"?type=f140");
                            break;
                        case "total":
                            if (b != null)
                                b.setTotal(parser.next());
                            break;
                        case "lprice":
                            if (b != null)
                                b.setLprice(Integer.parseInt(parser.nextText()));
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
    // end of jaeman

    public String RemoveHTMLTag(String changeStr) {
        if (changeStr != null && !changeStr.equals("")) {
            changeStr = changeStr.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "");
        } else {
            changeStr = "";
        }
        return changeStr;
    }

    public void insertAllAboutProduct(Results_GridItem aItem) {

        String cKey = aItem.getCombinationKeyword();
        ArrayList<String> keys = aItem.getKeywords();
        Log.i("cKey",cKey );
        Log.i("keys",keys.toString() );

        MainActivity.DBInstance.insertMatchingCombinationLocal(cKey, aItem.getThumbUrl());
        MainActivity.DBInstance.insertSearchedProduct(cKey, (new Date()).getTime(), 0,
                                                    aItem.getThumbUrl(), aItem.getPrice(), aItem.getUrl());

        for(int i=0; i < aItem.getKeywords().size(); i++) {
            MainActivity.DBInstance.insertKeywordCountLocal(keys.get(i), 0);
            MainActivity.DBInstance.insertKeywordInCombinationLocal(keys.get(i), cKey);
        }

        String[] params = new String[2];
        try {
            params[0] = cKey;
            params[1] = aItem.getThumbUrl();
            MainActivity.DBInstance.insertIntoServerTable("matching_combination", params);
            Thread.sleep(100);

            params[1] = "0";
            for(int i=0; i < aItem.getKeywords().size(); i++) {
                params[0] = keys.get(i);
                MainActivity.DBInstance.insertIntoServerTable("keyword_count", params);
                Thread.sleep(100);
            }
            params[0] = cKey;
            for(int i=0; i < aItem.getKeywords().size(); i++) {
                params[1] = keys.get(i);
                MainActivity.DBInstance.insertIntoServerTable("keyword_in_combination", params);
                Thread.sleep(100);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }


}
