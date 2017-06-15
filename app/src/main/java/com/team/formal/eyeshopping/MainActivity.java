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

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.WebDetection;
import com.google.api.services.vision.v1.model.WebImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    public static DBHelper DBInstance;
    public static final String FILE_NAME = "temp.jpg";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    public static final int SHOW_VISUALLY_SIMILAR_IMAGES_REQUEST = 4;
    public static final int RECENT_SEARCH_REQEST = 5;
    public static final int RECOMMEND_PRODUCT_REQUEST = 6;
    public static final int POPULAR_PRODUCT_REQUEST = 7;
    String[] PERMISSIONS = {"android.permission.CAMERA"};
    static final int PERMISSIONS_REQUEST_CODE = 1000;

    static final String DBName = "EyeShopping.db";

    // 갤러리 카메라에서 받은 이미지를 다음 액티비티로 넘겨주기 위한 URI
    public String our_uri;

    // View Pager 객체 전역 변수들
    Bitmap mResources[] = new Bitmap[4];
    int dotsCount;
    LinearLayout pager_indicator;
    ViewPager intro_images;
    ViewPagerAdapter mAdapter;
    ImageView dots[];
    final ArrayList<Bitmap> recommend_bitmaps = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // if you want to rest call this.deleteDatabase("EyeShopping.db")
        this.deleteDatabase(DBName); // initialize DB tables
        DBInstance = new DBHelper(this, DBName, null, 1);

//        DBInstance.insertSearchedProduct("test1 cKeyword", (new Date()).getTime(), 0);
//        DBInstance.insertSearchedProduct("test2 cKeyword", (new Date()).getTime(), 0);

//        Cursor c = DBInstance.getTuples("searched_product");
//        while(c.moveToNext()) {
//            Log.i("id", ""+c.getInt(0));
//            Log.i("cKeys", c.getString(1));
//            Log.i("date", ""+c.getInt(2));
//            boolean like = c.getInt(3) > 0;
//            Log.i("like", ""+like);
//        }

//        try {
//            ArrayList<String> keyNames = new ArrayList<>();
//            keyNames.add("nike");
//            keyNames.add("adidas");
//            keyNames.add("reebok");
//            DBInstance.getRecommendedUrls(keyNames, new AsyncResponse() {
//                @Override
//                public void processFinish(Object output) {
//                    // output이 받은 데이터이니 여기서 할거 하십쇼!
//                    testList = (ArrayList<HashMap>) output;
//
//                    for(int i=0; i < testList.size(); i++) {
//                        Log.i("testList "+i, "complete!");
//                        Log.i("combination_keyword "+i, (String) testList.get(i).get("combination_keyword"));
//                        Log.i("matching_image_url "+i, (String) testList.get(i).get("matching_image_url"));
//                    }
//                }
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        setContentView(R.layout.activity_main);
        makeLocalDummyData();
        setRecommendedProductList();
    }

    public void setRecommendedProductList() {
        SQLiteDatabase db = DBInstance.getReadableDatabase();

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
                DBInstance.getRecommendedUrls(liked_keywords, new AsyncResponse() {
                    @Override
                    public void processFinish(Object output) {
                        ArrayList<HashMap> outputList = (ArrayList<HashMap>) output;
                        ArrayList<String> keywords = new ArrayList<>();
                        final ArrayList<String> urls = new ArrayList<>();

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
                            final ProgressDialog asyncDialog =
                                    new ProgressDialog(MainActivity.this);

                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();

                                asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                asyncDialog.setMessage("Loading Recommend Products..");

                                // show dialog
                                asyncDialog.show();
                            }

                            @Override
                            protected ArrayList<Bitmap> doInBackground(Object... params) {
                                for (int i = 0; i < urls.size(); i++) {
                                    try {
                                        URL url = new URL(urls.get(i));
                                        Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                        recommend_bitmaps.add(bitmap);
                                        Log.d(TAG, "mResources: " + Integer.toString(recommend_bitmaps.size()));
                                    } catch (IOException e) {
                                        System.out.println(e);
                                    }
                                }

                                return recommend_bitmaps;
                            }

                            @Override
                            protected void onPostExecute(ArrayList<Bitmap> bitmaps) {
                                Bitmap logo = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                        R.drawable.splash);
                                mResources[0] = logo;
                                for(int i=0;i<recommend_bitmaps.size();i++)
                                {
                                    mResources[i+1] = recommend_bitmaps.get(i);
                                }

                                Bitmap default_bitmaps[] = new Bitmap[3];
                                default_bitmaps[0] = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                        R.drawable.view_example_1);
                                default_bitmaps[1] = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                        R.drawable.view_example_3);
                                default_bitmaps[2] = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                                        R.drawable.view_example_4);

                                int default_index=0;
                                for(int i=recommend_bitmaps.size()+1;i<mResources.length;i++)
                                {
                                    mResources[i] = default_bitmaps[default_index];
                                    default_index++;
                                }

                                // View Pager 초기 설정
                                intro_images = (ViewPager) findViewById(R.id.pager);
                                pager_indicator = (LinearLayout) findViewById(R.id.viewPagerCountDots);
                                mAdapter = new ViewPagerAdapter(getApplicationContext(), mResources);
                                intro_images.setAdapter(mAdapter);
                                intro_images.setCurrentItem(0);
                                intro_images.setOnPageChangeListener(MainActivity.this);
                                setUiPageViewController();

                                asyncDialog.dismiss();
                            }
                        }.execute();
                    }
                });
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

    /*
        View Pager 설정
     */
    private void setUiPageViewController() {
        dotsCount = mAdapter.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(getResources().getDrawable(R.drawable.main_pager_non_selecteditem_dot));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(4, 0, 4, 0);
            pager_indicator.addView(dots[i], params);
        }

        dots[0].setImageDrawable(getResources().getDrawable(R.drawable.main_pager_selecteditem_dot));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        for (int i = 0; i < dotsCount; i++) {
            dots[i].setImageDrawable(getResources().getDrawable(R.drawable.main_pager_non_selecteditem_dot));
        }

        dots[position].setImageDrawable(getResources().getDrawable(R.drawable.main_pager_selecteditem_dot));
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /*
        View Pager Adapter
     */
    public class ViewPagerAdapter extends PagerAdapter {
        private Context mContext;
        private Bitmap[] mResources;

        public ViewPagerAdapter(Context mContext, Bitmap[] mResources) {
            this.mContext = mContext;
            this.mResources = mResources;
        }

        @Override
        public int getCount() {
            return mResources.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((LinearLayout) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View itemView = LayoutInflater.from(mContext).inflate(R.layout.main_pager_item, container, false);

            ImageView imageView = (ImageView) itemView.findViewById(R.id.pager_item_image_view);
            imageView.setImageBitmap(mResources[position]);

            container.addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }

    /*
        Camera Button Click시 호출
     */
    public void onCameraButtonClick(View view) { startCamera(); }

    /*
        Gallery Button Click시 호출
     */
    public void onGalleryButtonClick(View view) {
        startGalleryChooser();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //퍼미션 상태 확인
            if (!hasPermissions(PERMISSIONS)) {

                //퍼미션 허가 안되어있다면 사용자에게 요청
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    private boolean hasPermissions(String[] permissions) {
        int result;

        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions) {

            result = ContextCompat.checkSelfPermission(this, perms);

            if (result == PackageManager.PERMISSION_DENIED) {
                //허가 안된 퍼미션 발견
                return false;
            }
        }

        //모든 퍼미션이 허가되었음
        return true;
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(getApplicationContext(),
                    getApplicationContext().getPackageName() + ".provider",
                    getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    /*
        최근 검색어 버튼 클릭시 호출
     */
    public void onRecentlySearchedListButtonClick(View view) {
        Intent intent = new Intent(getApplicationContext(), ActivityRecentSearch.class);
        startActivityForResult(intent, RECENT_SEARCH_REQEST);
    }

    /*
        추천 상품 버튼 클릭시 호출
     */
    public void onRecommendedProductListButtonClick(View view) {
        Intent intent = new Intent(getApplicationContext(), ActivityRecommendProducts.class);
        startActivityForResult(intent, RECOMMEND_PRODUCT_REQUEST);
    }

    /*
        인기 상품 버튼 클릭시 호출
     */
    public void onPopularSearchedListButtonClick(View view) {
        Intent intent = new Intent(getApplicationContext(), ActivityPopularProducts.class);
        startActivityForResult(intent, POPULAR_PRODUCT_REQUEST);
    }

    /*
        급상승 버튼 클릭시 호출
     */
    public void onGreatlyIncreasedProductListButtonClick(View view) {

    }

    /*
        Chile Activity 종료 후 돌아올 시 이벤트 처리 함수
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            bitmap = uploadImage(data.getData());

            Intent intent = new Intent(getApplicationContext(), ActivityShowVisuallySimilarImages.class);
            intent.putExtra("uri", our_uri);

            startActivityForResult(intent, SHOW_VISUALLY_SIMILAR_IMAGES_REQUEST);
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            bitmap = uploadImage(photoUri);

            Intent intent = new Intent(getApplicationContext(), ActivityShowVisuallySimilarImages.class);
            intent.putExtra("uri", our_uri);
            startActivityForResult(intent, SHOW_VISUALLY_SIMILAR_IMAGES_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public Bitmap uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                System.out.println("respones!" + uri.getPath());
                our_uri = uri.toString();
                return bitmap;
            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
                return null;
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            return null;
        }
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

    private String convertResponseToString(BatchAnnotateImagesResponse response) {

        String message = "I found these things!!!!!!!!:\n\n";
        WebDetection annotation = response.getResponses().get(0).getWebDetection();
        if (annotation != null) {
            System.out.println("Entity:Score");
            System.out.println("===============");
//            for (WebEntity entity : annotation.getWebEntities()) {
//                 message += String.format(Locale.KOREAN, "%.3f: %s", entity.getScore(), entity.getDescription());
//                message += "\n";
//                System.out.println(entity.getDescription() + " : "  + entity.getScore());
//            }
//            if(!annotation.getPagesWithMatchingImages().isEmpty()) {
//                System.out.println("\nPages with matching images: Score\n==");
//                for (WebPage page : annotation.getPagesWithMatchingImages()) {
//                    System.out.println(page.getUrl() + " : " + page.getScore());
//                    message += String.format(Locale.KOREAN, "%s: %s", page.getUrl(), page.getScore());
//                    message += "\n";
//                }
//            }
//            if(!annotation.getPartialMatchingImages().isEmpty()) {
//                System.out.println("\nPages with partially matching images: Score\n==");
//                for (WebImage image : annotation.getPartialMatchingImages()) {
//                    System.out.println(image.getUrl() + " : " + image.getScore());
//                }
//            }
//            if(!annotation.getFullMatchingImages().isEmpty()) {
//                System.out.println("\nPages with fully matching images: Score\n==");
//                for (WebImage image : annotation.getFullMatchingImages()) {
//
//                    System.out.println(image.getUrl() + " : " + image.getScore());
//                }
//            }
            for (WebImage image : annotation.getVisuallySimilarImages()) {
                System.out.println(image.getUrl() + " : " + image.getScore());
                message += String.format(Locale.KOREAN, "%s: %s", image.getUrl(), image.getScore());
                message += "\n";
            }
        } else {
            message += "nothing";
        }

        return message;
    }

    private String convertResponseToStringLabel(BatchAnnotateImagesResponse response) {

        String message = "I found these things!!!!!!!!:\n\n";
        java.util.List<EntityAnnotation> annotations = response.getResponses().get(0).getLabelAnnotations();
        if (annotations != null) {
            System.out.println("Entity:Score");
            System.out.println("===============");
            for (EntityAnnotation entity : annotations) {
                message += String.format(Locale.KOREAN, "%.3f: %s", entity.getScore(), entity.getDescription());
                message += "\n";
                System.out.println(entity.getDescription() + " : " + entity.getScore());
            }
        } else {
            message += "nothing";
        }

        return message;
    }

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
}