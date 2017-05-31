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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.WebDetection;
import com.google.api.services.vision.v1.model.WebImage;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String FILE_NAME = "temp.jpg";
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    //여기서부턴 퍼미션 관련 메소드
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    private static final String CLOUD_VISION_API_KEY = ""; // input ur key
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;

    public static final int SHOW_VISUALLY_SIMILAR_IMAGES_REQUEST = 4;
    static {
        System.loadLibrary("native-lib");
    }

    String[] PERMISSIONS = {"android.permission.CAMERA"};
    private TextView mImageDetails;
    private ImageView mMainImage;
    private Bitmap mNaverPrImg;
    private Mat userSelImg = null; // TODO

    //    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInput;
    private Mat matResult;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);

    public native void CornerHarrisDemo(long addrInputImage, long addrOutput);

    public native int AkazeFeatureMatching(long userSelImage, long naverPrImage);


    private ViewGroup mRelativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public void onCameraButtonClick(View view) {
        startCamera();
    }

    public void onGalleryButtonClick(View view) {
        startGalleryChooser();
        setContentView(R.layout.content_main);

        mRelativeLayout = (ViewGroup) findViewById(R.id.contentMain);

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
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            bitmap = uploadImage(data.getData());

            Intent intent = new Intent(getApplicationContext(), Activity_ShowVisuallySimilarImages.class);
            intent.putExtra("bitmap", bitmap);
            startActivityForResult(intent, SHOW_VISUALLY_SIMILAR_IMAGES_REQUEST);
        }
        else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            bitmap = uploadImage(photoUri);

            Intent intent = new Intent(getApplicationContext(), Activity_ShowVisuallySimilarImages.class);
            intent.putExtra("bitmap", bitmap);
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







// TODO
/*
        try {
            // TODO 이것도 naverPrImgTarget 처럼.. url로 처리
            userSelImg = Utils.loadResource(this, R.drawable.user_image, CvType.CV_8UC4); // return BGR 순
//            naverPrImg = Utils.loadResource(this, R.drawable.marmont_bag, CvType.CV_8UC4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // SET DUMMY DATA
        final Shop dummyShop = new Shop();
        dummyShop.setImage("http://shopping.phinf.naver.net/main_1144437/11444373299.jpg?type=f140");
        dummyShop.setTitle("Marmont Handbag");
        dummyShop.setLprice(2340958);

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

                for(int i=0; i<3; i++) {
                    View productLayout = LayoutInflater.from(getBaseContext()).inflate(R.layout.product, mRelativeLayout, false);

                    TextView productName = (TextView) productLayout.findViewById(R.id.productName);
                    productName.setText(dummyShop.getTitle());

                    ImageView productThumb = (ImageView) productLayout.findViewById(R.id.Thumbnail);
                    productThumb.setImageBitmap(mNaverPrImg);

                    TextView productPrice = (TextView) productLayout.findViewById(R.id.price);
                    productPrice.setText(String.valueOf(dummyShop.getLprice()));

                    mRelativeLayout.addView(productLayout);
                }

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
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
*/