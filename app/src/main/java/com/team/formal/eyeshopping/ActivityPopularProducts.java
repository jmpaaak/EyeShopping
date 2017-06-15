package com.team.formal.eyeshopping;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class ActivityPopularProducts extends AppCompatActivity {

    private static final String TAG = ActivityPopularProducts.class.getSimpleName();

    public static final int SELECT_REQUEST = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popular_products);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Listview Setting
        ListView listview = (ListView)findViewById(R.id.popular_product_list_view);

        ArrayList<PopularProduct_ListItem> listItems = new ArrayList<>();

        listItems.add(new PopularProduct_ListItem("1", "나이키", 6000));
        listItems.add(new PopularProduct_ListItem("2", "가죽백", 4000));
        listItems.add(new PopularProduct_ListItem("3", "나이키", 325));
        listItems.add(new PopularProduct_ListItem("4", "가죽백", 23));
        listItems.add(new PopularProduct_ListItem("5", "나이키", 22));
        listItems.add(new PopularProduct_ListItem("6", "가죽백", 11));
        listItems.add(new PopularProduct_ListItem("7", "나이키", 4));
        listItems.add(new PopularProduct_ListItem("8", "가죽백", 1));
        listItems.add(new PopularProduct_ListItem("9", "나이키", 1));
        listItems.add(new PopularProduct_ListItem("10", "가죽백", 1));

        PopularProduct_ListViewAdapter listViewAdapter = new PopularProduct_ListViewAdapter(this, listItems);

        listview.setAdapter(listViewAdapter);

        // 기준 날짜 셋팅
        TextView popular_date = (TextView)findViewById(R.id.popular_date);

        Calendar c = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss 기준");
        String formattedDate = df.format(c.getTime());

        popular_date.setText(formattedDate);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /*
        리스트 어댑터, 리스트 뷰를 inflate하여 객체화 한다
     */
    private class PopularProduct_ListViewAdapter extends BaseAdapter {
        Context context;
        ArrayList<PopularProduct_ListItem> listItems;

        private PopularProduct_ListViewAdapter(Context context, ArrayList<PopularProduct_ListItem> listItems) {
            this.context = context;
            this.listItems = listItems;
        }

        public int getCount() {
            return listItems.size();
        }

        public PopularProduct_ListItem getItem(int position) {
            return listItems.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PopularProduct_ListView listView;

            if(convertView == null) {
                listView = new PopularProduct_ListView(this.context, this.listItems.get(position));
            } else {
                listView = (PopularProduct_ListView) convertView;
            }

            return listView;
        }
    }

    /*
        리스트뷰 아이템, 리스트 뷰 항목 하나 하나에 들어갈 정보를 담고 있다
     */
    private class PopularProduct_ListItem {
        private String number;
        private String keyword;
        private int count;

        public PopularProduct_ListItem(String number, String keyword, int count) {
            this.number = number;
            this.keyword = keyword;
            this.count = count;
        }

        public String getNumber() { return this.number; }
        public String getKeyword() { return this.keyword; }
        public int getCount() { return this.count; }

        public void setNumber(String number) { this.number = number; }
        public void setKeyword(String keyword) { this.keyword= keyword; }
        public void setCount(int count) { this.count = count; }

    }

    /*
        리스트뷰 뷰, xml과 연결된 레이아웃 클래스
     */
    private class PopularProduct_ListView extends LinearLayout {

        private TextView number_text;
        private TextView keyword_text;
        private TextView count_text;

        public PopularProduct_ListView(Context context, final PopularProduct_ListItem aItem) {
            super(context);

            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.popular_list_view_item, this, true);

            number_text = (TextView)findViewById(R.id.popular_product_number_text);
            number_text.setText(aItem.getNumber());

            keyword_text = (TextView)findViewById(R.id.popular_product_keyword_text);
            keyword_text.setText(aItem.getKeyword());

            count_text = (TextView)findViewById(R.id.popular_product_count);
            DecimalFormat df = new DecimalFormat("#,###");
            String num = df.format(aItem.getCount());
            String res = num+" 조회수";
            count_text.setText(res);
        }
    }

}
