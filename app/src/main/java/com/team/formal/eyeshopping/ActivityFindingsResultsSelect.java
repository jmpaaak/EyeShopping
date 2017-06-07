package com.team.formal.eyeshopping;

import android.content.Intent;
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

import java.text.DecimalFormat;

/**
 * Created by NaJM on 2017. 6. 5..
 */

public class ActivityFindingsResultsSelect extends AppCompatActivity {
    private static final int NEXT_REQUEST = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finding_results_select);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button cancelButton = (Button) findViewById(R.id.selection_cancel);
        Button shoppingButton = (Button) findViewById(R.id.selection_shopping);

        Intent intent = getIntent();

        //searchDate -> DB에 넣을것
        //ActivityFindingsResults에서 넘겨받음
        String getTime = intent.getStringExtra("date");
        //Log.i("AAAAA",getTime);

        //TEST
        ImageView imageView = (ImageView) findViewById(R.id.view_image);
        imageView.setImageResource(R.drawable.marmont_bag);
        TextView textView = (TextView) findViewById(R.id.Product_name);
        TextView textView1 = (TextView) findViewById(R.id.Product_price);
        textView.setText("Marmont Handbag");
        DecimalFormat df = new DecimalFormat("#,###");
        String num = df.format(123456);
        textView1.setText("최저가 : " +num+"원");


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
        CheckBox checkbox = (CheckBox) findViewById(R.id.checkbox);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //db 저장
                    //앞에서 id 넘겨주고 여기다가 update
                    //DBInstance.updateSearchedProductLike(ID,1);
                }
                else{
                    //db 저장
                    //앞에서 id 넘겨주고 여기다가 update
                    //DBInstance.updateSearchedProductLike(ID,0);

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
                //http://openapi.naver.com/search/?key=머시기&query=노트북&display=10&start=1&target=shop&sort=asc
                //key = 이용등록을 통해 받은 key string
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://sports.news.naver.com/index.nhn"));
//                intent.putExtra("url", url); //  웹 url
//                intent.putExtra("uri", uri.toString()); // local uri
                startActivityForResult(intent, NEXT_REQUEST);
            }
        });

    }
}
