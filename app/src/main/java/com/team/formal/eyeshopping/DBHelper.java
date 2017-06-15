package com.team.formal.eyeshopping;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String TAG = DBHelper.class.getSimpleName();
    private static final String serverURL = "http://54.251.159.248/eyeshopping/";
    private static final String TAG_JSON="webnautes";

    private ArrayList<HashMap> mAttrsList = new ArrayList<>();

    // DBHelper 생성자로 관리할 DB 이름과 버전 정보를 받음
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.context = context;
    }

    // DB를 새로 생성할 때 호출되는 함수
    @Override
    public void onCreate(SQLiteDatabase db) {
        // matching_combination_local
        db.execSQL("CREATE TABLE matching_combination_local (combination_keyword TEXT PRIMARY KEY," +
                "matching_image_url TEXT);");

        // searched_product
        db.execSQL("CREATE TABLE searched_product (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "combination_keyword TEXT, " +
                "search_date LONG, " +
                "like BOOLEAN, " +
                "selected_image_url TEXT, " +
                "price INTEGER, " +
                "mall_url TEXT, " +
                "FOREIGN KEY(combination_keyword) REFERENCES matching_combination_local(combination_keyword));");

        // keyword_name
        db.execSQL("CREATE TABLE keyword_count_local (keyword_name TEXT PRIMARY KEY, " +
                "count INTEGER);");

        // keyword_in_combination_local
        db.execSQL("CREATE TABLE keyword_in_combination_local (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "keyword_name TEXT, " +
                "combination_keyword TEXT, " +
                "FOREIGN KEY(keyword_name) REFERENCES keyword_count_local(keyword_name), " +
                "FOREIGN KEY(combination_keyword) REFERENCES matching_combination_local(combination_keyword));");
    }

    // DB 업그레이드를 위해 버전이 변경될 때 호출되는 함수
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }


    /***** INSERT INTO TABLE *****/

    public void insertSearchedProduct(String combinationKeyword, long searchDate, int like, String selectedImageUrl, int price, String mall_url) { // like 0=false, 1=true
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO searched_product VALUES(null, '" + combinationKeyword + "', " + searchDate + ", " + like + ", '" + selectedImageUrl + "', "+ price + ", '" + mall_url + "');");
        Log.i(combinationKeyword, " - insertSearchedProduct complete!");
    }

    public void insertMatchingCombinationLocal(String combinationKeyword, String matchingImageUrl) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO matching_combination_local VALUES('" + combinationKeyword + "', '" + matchingImageUrl + "');");
        Log.i(combinationKeyword, " - insertMatchingCombinationLocal complete!");
    }

    public void insertKeywordCountLocal(String keywordName, int count) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO keyword_count_local VALUES('" + keywordName + "', " + count + ");");
        Log.i(keywordName, " - insertKeywordCountLocal complete!");
    }

    public void insertKeywordInCombinationLocal(String keywordName, String combinationKeyword) {
        SQLiteDatabase db = getWritableDatabase();

        //Cursor cursor = db.rawQuery("SELECT * FROM keyword_count_local WHERE keyword_name= "+ keywordName, null);
        if(true) {
            db.execSQL("INSERT INTO keyword_in_combination_local VALUES(null, '" + keywordName + "', '" + combinationKeyword + "');");
            Log.i(keywordName, " - insertKeywordInCombinationLocal complete!");
        } else {
            /*
            int newCount = cursor.getInt(1) + 1;
            db.execSQL("UPDATE keyword_count_local SET count=" + newCount + ", " +
                    " WHERE keyword_name='" + keywordName + "';");
            Log.i(keywordName, " - updateKeywordCountLocalCount complete!");
            */
        }
    }


    /***** UPDATE TABLE *****/

    public void updateSearchedProductLike(int id, int like) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE searched_product SET like=" + like + ", " +
                " WHERE _id=" + id + ";");
        Log.i(id+"", " - updateSearchedProductLike complete!");
    }


    /***** SELECT TABLE *****/

    public Cursor getTuples(String tableName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM '" + tableName + "'", null);

        return cursor;
    }


    /***** SERVER METHODS *****/

    public void insertIntoServerTable(final String tableName, final String[] postData) throws IOException {
        ArrayList<HashMap> retList = new ArrayList<>();
        new AsyncTask<String, Void, String>() {
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute () {
                super.onPreExecute();

                progressDialog = ProgressDialog.show(context,
                        "데이터를 서버에 전송 중입니다..", null, true, true);
            }

            @Override
            protected void onPostExecute (String result){
                super.onPostExecute(result);

                progressDialog.dismiss();
                Log.d(TAG, "POST response  - " + result);
            }

            @Override
            protected String doInBackground (String[] params){

                // set post params
                String postParameters = null;
                String combination_keyword;
                String matching_image_url;
                String keyword_name;
                String count;
                switch (tableName) {
                    case "matching_combination":
                        combination_keyword = postData[0];
                        matching_image_url = postData[1];

                        // set POST params
                        postParameters = "combination_keyword=" + combination_keyword
                                         + "&matching_image_url=" + matching_image_url;
                        break;
                    case "keyword_in_combination":
                        keyword_name = postData[0];
                        combination_keyword = postData[1];

                        // set POST params
                        postParameters = "keyword_name=" + keyword_name
                                        + "&combination_keyword=" + combination_keyword;
                        break;
                    case "keyword_count":
                        keyword_name = postData[0];
                        count = postData[1];

                        // set POST params
                        postParameters = "keyword_name=" + keyword_name
                                         + "&count=" + count;
                        break;
                }

                try {
                    // set .php file
                    URL url = new URL(serverURL + "insert_" + tableName + ".php");
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                    httpURLConnection.setReadTimeout(5000);
                    httpURLConnection.setConnectTimeout(5000);
                    httpURLConnection.setRequestMethod("POST");
                    //httpURLConnection.setRequestProperty("content-type", "application/json");
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.connect();

                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    outputStream.write(postParameters.getBytes("UTF-8"));
                    outputStream.flush();
                    outputStream.close();

                    int responseStatusCode = httpURLConnection.getResponseCode();
                    Log.d(TAG, "POST response code - " + responseStatusCode);

                    InputStream inputStream;
                    if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                        inputStream = httpURLConnection.getInputStream();
                    } else {
                        inputStream = httpURLConnection.getErrorStream();
                    }

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    StringBuilder sb = new StringBuilder();
                    String line = null;

                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }
                    bufferedReader.close();
                    return sb.toString();
                } catch (Exception e) {
                    Log.d(TAG, "InsertData: Error ", e);
                    return new String("Error: " + e.getMessage());
                }

            }
        }.execute(); // end of AsyncTask
    } // end of insertIntoServerTable

    public void getServerTable(final String tableName, final AsyncResponse asyncResponse) throws IOException {
        new AsyncTask<String, Void, String>() {
            ProgressDialog progressDialog;

            @Override
            protected void onPreExecute () {
                super.onPreExecute();

                progressDialog = ProgressDialog.show(context,
                        "서버 접속 중 입니다..", null, true, true);
            }

            @Override
            protected void onPostExecute (String result) {
                super.onPostExecute(result);

                progressDialog.dismiss();
                Log.d(TAG, "response  - " + result);

                if (result != null) {
                    setAttrsList(tableName, result);
                    asyncResponse.processFinish(mAttrsList);
                }
            }

            @Override
            protected String doInBackground (String[] params) {

                try {
                    // set .php file
                    URL url = new URL(serverURL + "get_" + tableName + ".php");
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                    httpURLConnection.setReadTimeout(5000);
                    httpURLConnection.setConnectTimeout(5000);
                    httpURLConnection.connect();

                    int responseStatusCode = httpURLConnection.getResponseCode();
                    Log.d(TAG, "response code - " + responseStatusCode);

                    InputStream inputStream;
                    if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                        inputStream = httpURLConnection.getInputStream();
                    }
                    else {
                        inputStream = httpURLConnection.getErrorStream();
                    }

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while((line = bufferedReader.readLine()) != null){
                        sb.append(line);
                    }

                    bufferedReader.close();
                    return sb.toString().trim();

                } catch (Exception e) {
                    Log.d(TAG, "getData: Error ", e);
                    return null;
                }

            }
        }.execute(); // end of AsyncTask
    } // end of insertIntoServerTable

    private void setAttrsList(String tableName, String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            // init List
            mAttrsList = new ArrayList<>();

            for(int i=0; i < jsonArray.length(); i++){
                HashMap<String,String> hashMap = new HashMap<>();
                JSONObject item = jsonArray.getJSONObject(i);
                String combination_keyword;
                String matching_image_url;
                String keyword_name;
                String count;

                switch (tableName) {
                    case "matching_combination":
                        combination_keyword = item.getString("combination_keyword");
                        matching_image_url = item.getString("matching_image_url");

                        hashMap.put("combination_keyword", combination_keyword);
                        hashMap.put("matching_image_url", matching_image_url);
                        break;
                    case "keyword_in_combination":
                        keyword_name = item.getString("keyword_name");
                        combination_keyword = item.getString("combination_keyword");

                        hashMap.put("keyword_name", keyword_name);
                        hashMap.put("combination_keyword", combination_keyword);
                        break;
                    case "keyword_count":
                        keyword_name = item.getString("keyword_name");
                        count = item.getString("count");

                        hashMap.put("keyword_name", keyword_name);
                        hashMap.put("count", count);
                        break;
                    case "recommended_urls": // JOIN
                        combination_keyword = item.getString("combination_keyword");
                        matching_image_url = item.getString("matching_image_url");

                        hashMap.put("combination_keyword", combination_keyword);
                        hashMap.put("matching_image_url", matching_image_url);
                        break;
                }
                mAttrsList.add(hashMap);
            } // end of for
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, " - setAttrsList : ", e);
        }
    }

    public void getRecommendedUrls(final ArrayList<String> keywordNameList, final AsyncResponse asyncResponse) throws IOException
    {
        final String join_table_name = "recommended_urls";
        final ProgressDialog progressDialog = new ProgressDialog(this.context);

        new AsyncTask<String, Void, String>() {

            @Override
            protected void onPreExecute () {
                super.onPreExecute();
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("Data loading ...");
                progressDialog.show();
            }

            @Override
            protected void onPostExecute (String result) {
                super.onPostExecute(result);

                progressDialog.dismiss();
                Log.d(TAG, "response  - " + result);

                if (result != null) {
                    setAttrsList(join_table_name, result);
                    asyncResponse.processFinish(mAttrsList);
                }
            }

            @Override
            protected String doInBackground (String[] params) {
                try {
                    // set .php file
                    URL url = new URL(serverURL + "get_" + join_table_name + ".php");
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                    httpURLConnection.setReadTimeout(5000);
                    httpURLConnection.setConnectTimeout(5000);
                    httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.connect();

                    String postParameters = "";
                    postParameters += "keywordNames[]=" + keywordNameList.get(0);
                    keywordNameList.remove(0);
                    for (String keyword : keywordNameList) {
                        postParameters += "&keywordNames[]=" + keyword;
                    }

//                    connection.setRequestMethod("POST");
//
//                    request = new OutputStreamWriter(connection.getOutputStream());
//                    request.write(parameters);
//                    request.flush();
//                    request.close();


                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    outputStream.write(postParameters.getBytes("UTF-8"));
                    outputStream.flush();
                    outputStream.close();

                    int responseStatusCode = httpURLConnection.getResponseCode();
                    Log.d(TAG, "response code - " + responseStatusCode);

                    InputStream inputStream;
                    if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                        inputStream = httpURLConnection.getInputStream();
                    }
                    else {
                        inputStream = httpURLConnection.getErrorStream();
                    }

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while((line = bufferedReader.readLine()) != null){
                        sb.append(line);
                    }

                    bufferedReader.close();
                    return sb.toString().trim();

                } catch (Exception e) {
                    Log.d(TAG, "getData: Error ", e);
                    return null;
                }

            }
        }.execute(); // end of AsyncTask
    } // end of insertIntoServerTable


}