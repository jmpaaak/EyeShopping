package com.team.formal.eyeshopping;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by NaJM on 2017. 6. 5..
 */

public class ActivityFindingsResultsSelect extends AppCompatActivity {
    private static final int NEXT_REQUEST = 1000;
    private String productUrl;
    private String mallUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finding_results_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button cancelButton = (Button) findViewById(R.id.selection_cancel);
        Button shoppingButton = (Button) findViewById(R.id.selection_shopping);

        final Intent intent = getIntent();

        //searchDate -> DB에 넣을것
        //ActivityFindingsResults에서 넘겨받음
        Bitmap thumb = (Bitmap) intent.getParcelableExtra("product_thumbnail");
        String productName = (String) intent.getSerializableExtra("product_name");
        //Log.i("pName", productName);
        String price = (String) intent.getSerializableExtra("product_price");
        productUrl = (String) intent.getSerializableExtra("product_url");

        //TEST
        ImageView imageView = (ImageView) findViewById(R.id.view_image);
        imageView.setImageBitmap(thumb);
        TextView textView = (TextView) findViewById(R.id.Product_name);
        TextView textView1 = (TextView) findViewById(R.id.Product_price);
        textView.setText(productName);
        textView1.setText(price);

        Cursor c = MainActivity.DBInstance.getTuples("searched_product");
        while(c.moveToNext()) {
            if(c.getString(6).equals(productUrl) && c.getInt(3) == 1) {
                CheckBox likeBox = (CheckBox) findViewById(R.id.likeBox);
                likeBox.setChecked(true);
            }
        }

//        // Url과 Uri를 부모 액티비티에서 받는다.
//        final String url = intent.getStringExtra("sample");
//        final Uri uri = Uri.parse(intent.getStringExtra("uri"));

        // get Bitmap image from uri
//        try {
//            // 비트맵을 내부 저장소에서 Uri를 활용해 얻는다
//            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//            // 이미지 뷰에 띄운다
//            ImageView imageView = (ImageView) findViewById(R.id.view_image);
//            imageView.setImageBitmap(bitmap);
//        } catch (FileNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        CheckBox checkbox = (CheckBox) findViewById(R.id.likeBox);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int curID = 0;
                if(isChecked) {
                    //db 저장
                    //앞에서 id 넘겨주고 여기다가 update
                    Cursor c = MainActivity.DBInstance.getTuples("searched_product");
                    while(c.moveToNext()) {
                        if(c.getString(6).equals(productUrl)) {
                            curID = c.getInt(0);
                        }
                    }
                    MainActivity.DBInstance.updateSearchedProductLike(curID, 1);
                }
                else{
                    //db 저장
                    //앞에서 id 넘겨주고 여기다가 update
                    Cursor c = MainActivity.DBInstance.getTuples("searched_product");
                    while(c.moveToNext()) {
                        if(c.getString(6).equals(productUrl)) {
                            curID = c.getInt(0);
                        }
                    }
                    MainActivity.DBInstance.updateSearchedProductLike(curID, 0);

                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
//
        shoppingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Recent에서 오는 mallUrl ( DB 완성 전 TEST용 )
                //    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mallUrl));

                //ResultsSelect에서 오는 productUrl
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(productUrl));
                startActivityForResult(intent, NEXT_REQUEST);

//                intent.putExtra("url", url); //  웹 url
//                intent.putExtra("uri", uri.toString()); // local uri
               // startActivityForResult(intent, NEXT_REQUEST);
            }
        });

    }
}
