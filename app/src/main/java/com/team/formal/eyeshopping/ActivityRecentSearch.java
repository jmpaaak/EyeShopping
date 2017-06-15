package com.team.formal.eyeshopping;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;

import static com.team.formal.eyeshopping.MainActivity.DBInstance;

/**
 * Created by NaJM on 2017. 6. 4..
 */

public class ActivityRecentSearch extends AppCompatActivity {
    private static final int SELECT_REQUEST = 2000;

    // Grid View 전역 변수
    GridView gridView;
    GridViewAdapter gridViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //URL , 키워드 조합 받아와서 INSERT 할 것!!!!!
        DBInstance.insertSearchedProduct("Marmont Red Gucci", 123, 1, "http://shopping.phinf.naver.net/main_1144437/11444373299.jpg?type=f140",7777777,"http://www.naver.com");
        DBInstance.insertSearchedProduct("Marmont Red Gucci", 123, 1, "http://shopping.phinf.naver.net/main_1144437/11444373299.jpg?type=f140",7777777,"http://www.naver.com");
        DBInstance.insertSearchedProduct("Marmont Red Gucci", 123, 1, "http://shopping.phinf.naver.net/main_1144437/11444373299.jpg?type=f140",7777777,"http://www.naver.com");
        DBInstance.insertSearchedProduct("Marmont Red Gucci", 123, 1, "http://shopping.phinf.naver.net/main_1144437/11444373299.jpg?type=f140",7777777,"http://www.naver.com");

        final Cursor c = DBInstance.getTuples("searched_product");
        c.moveToNext();
        final int count = c.getCount();

        new AsyncTask<Object, Void, ArrayList<Results_GridItem>>() {
            @Override
            protected ArrayList<Results_GridItem> doInBackground(Object... params) {
                ArrayList<Results_GridItem> gridItems = new ArrayList<>();
                HttpURLConnection connection = null;
                for(int i=0; i<count; i++) {
                    String keyword;
                    Bitmap bitmap;
                    String price;
                    String mall_url;
                    URL url;
                    try {
                        url = new URL(c.getString(4));
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        BitmapFactory.Options op = new BitmapFactory.Options();
                        op.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        bitmap = BitmapFactory.decodeStream(input, null, op);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    } finally {
                        if (connection != null) connection.disconnect();
                    }
                    keyword = c.getString(1);
                    price = c.getString(5);
                    mall_url = c.getString(6);
                    DecimalFormat df = new DecimalFormat("#,###");
                    String num = df.format(Integer.parseInt(price));
                    gridItems.add(new Results_GridItem(bitmap,keyword,"최저가 " + num+ "원",url.toString(),mall_url));
                    if( c.isLast()) { break; }
                    else { c.moveToNext(); }
                }
                c.close();
                return gridItems;
            }

            protected void onPostExecute(ArrayList<Results_GridItem> gridItems)
            {
                gridView = (GridView)findViewById(R.id.recent_grid_view);
                gridViewAdapter = new GridViewAdapter(getApplicationContext(), gridItems);
                gridView.setAdapter(gridViewAdapter);
            }
        }.execute();
    }

    /*
        그리드뷰 어댑터, 그리드 뷰를 inflate하여 객체화 한다
     */
    private class GridViewAdapter extends BaseAdapter {
        Context context;
        ArrayList<ActivityRecentSearch.Results_GridItem> gridItems;

        private GridViewAdapter(Context context, ArrayList<ActivityRecentSearch.Results_GridItem> gridItems) {
            this.context = context;
            this.gridItems = gridItems;
        }

        public int getCount() {
            return gridItems.size();
        }

        public ActivityRecentSearch.Results_GridItem getItem(int position) {
            return gridItems.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ActivityRecentSearch.Results_GridView gridView;

            if(convertView == null) {
                gridView = new ActivityRecentSearch.Results_GridView(this.context, this.gridItems.get(position));
            } else {
                gridView = (ActivityRecentSearch.Results_GridView) convertView;
            }

            return gridView;
        }
    }

    /*
        그리드뷰 아이템 그리드 뷰에 들어갈 정보를 담고 있다
     */
    private class Results_GridItem {
        Bitmap bitmap;
        String keyword;
        String price;
        private int int_price;
        private String url;
        private String mall_url;
        public Results_GridItem(Bitmap bitmap, String keyword,String price, String url, String mall_url) {
            this.bitmap=bitmap;
            this.keyword=keyword;
            this.price=price;
            this.url=url;
            this.mall_url =mall_url;
        }
        public int getPrice() { return this.int_price; }
        public String getPriceText() { return this.price; }
        public Bitmap getThumb() { return this.bitmap; }
        public String getProductName() { return this.keyword; }
        public String getUrl() { return this.url; }
        public String getMall_url() {return this.mall_url; }
    }

    /*
        그리드뷰 뷰, xml과 연결된 레이아웃 클래스
     */
    private class Results_GridView extends LinearLayout {
        private ViewGroup vGroup;
        private ImageView thumbView; //이미지
        private TextView strView; //이름
        private TextView priceView;

        public Results_GridView(Context context, final ActivityRecentSearch.Results_GridItem aItem) {
            super(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.grid_recent_item, this, true);

            vGroup = (ViewGroup) findViewById(R.id.grid_recent_item_list_view);
            thumbView = (ImageView) findViewById(R.id.recent_product_thumbnail);
            thumbView.setImageBitmap(aItem.bitmap);
            strView = (TextView) findViewById(R.id.recent_str);
            strView.setText(String.valueOf(aItem.keyword));

            vGroup.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), ActivityFindingsResultsSelect.class);

                    //이미지 가격 이름 url 보내
                    ViewGroup vg = (ViewGroup) v;
                    thumbView = (ImageView) vg.findViewById(R.id.product_thumbnail);
                    strView = (TextView) vg.findViewById(R.id.product_name);
                    priceView = (TextView) vg.findViewById(R.id.product_price);

                    intent.putExtra("product_thumbnail", aItem.getThumb()); // 상품 이미지
                    intent.putExtra("product_name", aItem.getProductName()); // 상품 이름
                    intent.putExtra("product_price", aItem.getPriceText()); // 상품 가격
                    intent.putExtra("product_url", aItem.getUrl()); // 이미지 url
                    intent.putExtra("mall_url", aItem.getMall_url()); // 쇼핑 url
                    startActivityForResult(intent, 17777);
                }
            });

        }
    }
}
