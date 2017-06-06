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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ActivityFindingResults extends Activity {

    private static final String TAG = ActivityFindingResults.class.getSimpleName();
    private ViewGroup mRelativeLayout;
    private Bitmap mNaverPrImg;
    private Mat userSelImg = null;

    // Grid View 전역 변수
    GridView gridView;
    GridViewAdapter gridViewAdapter;

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

        gridView = (GridView) findViewById(R.id.list_view);

        try {
            // TODO 이것도 naverPrImgTarget 처럼.. url로 처리
            userSelImg = Utils.loadResource(this, R.drawable.user_image, CvType.CV_8UC4); // return BGR 순
            // naverPrImg = Utils.loadResource(this, R.drawable.marmont_bag, CvType.CV_8UC4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // SET DUMMY DATA
        final Shop dummyShop = new Shop();
        dummyShop.setImage("http://shopping.phinf.naver.net/main_1144437/11444373299.jpg?type=f140");
        dummyShop.setTitle("Marmont Handbag");
        DecimalFormat df = new DecimalFormat("#,###");
        String num = df.format(123456);
        dummyShop.setLprice("최저가 " +num+"원");
        // Thread로 웹서버에 접속
        new Thread() {
            public void run() {
                mNaverPrImg = getBitmapFromURL(dummyShop.getImage()); // 입력 이미지 Url

                Bundle bun = new Bundle();
                bun.putSerializable("productInfo", dummyShop);
                Message msg = detectHandler.obtainMessage();
                msg.setData(bun);
                detectHandler.sendMessage(msg);
            }
        }.start();
    }

    Handler detectHandler = new Handler() {
        public void handleMessage(Message msg) {

            Bundle bun = msg.getData();
            Shop dummyShop = (Shop) bun.getSerializable("productInfo");

            Mat userSelImgTarget = new Mat(userSelImg.width(), userSelImg.height(), CvType.CV_8UC4);
            Mat naverPrImgTarget = new Mat(mNaverPrImg.getWidth(), mNaverPrImg.getHeight(), CvType.CV_8UC4);

            Utils.bitmapToMat(mNaverPrImg, naverPrImgTarget);

            Imgproc.cvtColor(userSelImg, userSelImgTarget, Imgproc.COLOR_BGR2RGB);

            Imgproc.cvtColor(naverPrImgTarget, naverPrImgTarget, Imgproc.COLOR_RGBA2RGB);

            int ret = AkazeFeatureMatching(userSelImgTarget.getNativeObjAddr(),
                    naverPrImgTarget.getNativeObjAddr());

            if(ret == 1) { // find one!

                ArrayList<Results_GridItem> items = new ArrayList<>();

                for(int i=0; i<20; i++) {
                    items.add(new Results_GridItem(dummyShop.getTitle(),
                                                    mNaverPrImg,
                                                    dummyShop.getLprice()));
                }

                gridViewAdapter = new GridViewAdapter(getApplicationContext(), items);
                gridView.setAdapter(gridViewAdapter);

            } else {
                // goto next thumbnail img or next comb. keyword
            }

            // Bitmap bmp = Bitmap.createBitmap(addrOutput.cols(), addrOutput.rows(), Bitmap.Config.ARGB_8888);
            // Utils.matToBitmap(addrOutput, bmp);

            // mMainImage.setImageBitmap(bmp);

            Log.i("complete", "complete");
        }
    };

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
    class Results_GridItem {
        private Bitmap thumb;
        private String productName;
        private String price;

        public Results_GridItem(String productName, Bitmap thumb, String price) {
            this.thumb = thumb;
            this.productName = productName;
            this.price = price;
        }
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

            thumbView.setImageBitmap(aItem.thumb);
            productNameView.setText(aItem.productName);
            priceView.setText(String.valueOf(aItem.price));

            vGroup.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ActivityFindingsResultsSelect.class);

                    long now = System.currentTimeMillis();
                    Date date= new Date(now);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    String getTime = sdf.format(date);
                    intent.putExtra("date", getTime);
                    startActivityForResult(intent, 77777);
                }
            });
        }
    }

}
