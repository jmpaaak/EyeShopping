package com.team.formal.eyeshopping;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by jongmin on 2017-06-04.
 */

public class DBHelper extends SQLiteOpenHelper {
    private Context context;

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

    public void insertSearchedProduct(String combinationKeyword, long searchDate, int like, String selectedImageUrl) { // like 0=false, 1=true
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO searched_product VALUES(null, '" + combinationKeyword + "', " + searchDate + ", " + like + ", '" + selectedImageUrl + "');");
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
        db.execSQL("INSERT INTO keyword_in_combination_local VALUES(null, '" + keywordName + "', '" + combinationKeyword + "');");
        Log.i(keywordName, " - insertKeywordInCombinationLocal complete!");
    }


    /***** UPDATE TABLE *****/

    public void updateSearchedProductLike(int id, boolean like) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE searched_product SET like=" + like + ", " +
                " WHERE _id=" + id + ";");
        Log.i(id+"", " - updateSearchedProductLike complete!");
    }

    public void addKeywordCountLocalCount(String keywordName) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM keyword_count_local WHERE keyword_name='" + keywordName, null);
        int newCount = cursor.getInt(1) + 1;
        db.execSQL("UPDATE keyword_count_local SET count=" + newCount + ", " +
                " WHERE keyword_name='" + keywordName + "';");
        Log.i(keywordName, " - updateKeywordCountLocalCount complete!");
    }


    /***** SELECT TABLE *****/

    public Cursor getTuples(String tableName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM '" + tableName + "'", null);

        return cursor;
    }


    //    public void delete(int id) {
//        SQLiteDatabase db = getWritableDatabase();
//
//        // 입력한 id와 일치하는 행 삭제
//        db.execSQL("DELETE FROM PLANS WHERE _id=" + id + ";");
//
//        db.close();
//    }

//    public void deleteWithRepeatGroupId(Plan firstPlanDeleted) {
//        SQLiteDatabase db = getWritableDatabase();
//
//        Calendar temp = Calendar.getInstance();
//        temp.setTimeInMillis(firstPlanDeleted.dateInMillis);
//
//        // 입력한 repeatGroupId와 일치하고 선택 목표와 함께 이후 목표들의 행 삭제
//        db.execSQL("DELETE FROM PLANS WHERE repeatGroupId='" + firstPlanDeleted.repeatGroupId + "' AND dateInMillis >=" +
//                + firstPlanDeleted.dateInMillis + ";");
//
//        db.close();
//    }
//
//
//    public ArrayList<Plan> getResultWithRepeatGroupId(Plan planSelected) {
//        // 읽기가 가능하게 DB 열기
//        SQLiteDatabase db = getReadableDatabase();
//        ArrayList<Plan> arrayList = new ArrayList<Plan>();
//
//        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용
//        Cursor cursor = db.rawQuery("SELECT * FROM PLANS WHERE repeatGroupId='" + planSelected.repeatGroupId
//                + "' AND dateInMillis >=" +
//                + planSelected.dateInMillis + " ORDER BY dateInMillis ASC;", null);
//        while (cursor.moveToNext()) {
//
//            Plan plan = new Plan();
//            plan.id = cursor.getInt(0);
//            plan.planName = cursor.getString(1);
//            plan.dateInMillis = cursor.getLong(2);
//            plan.startAngle = cursor.getFloat(3);
//            plan.endAngle = cursor.getFloat(4);
//            plan.color = cursor.getInt(5);
//            plan.percentageOfAchieve = cursor.getInt(6);
//            plan.repeatTerm = cursor.getInt(7);
//            plan.repeatGroupId = cursor.getString(8);
//
//            arrayList.add(plan);
//        }
//
//        return arrayList;
//    }
//
//    public ArrayList<Plan> getResultAll() {
//        // 읽기가 가능하게 DB 열기
//        SQLiteDatabase db = getReadableDatabase();
//        ArrayList<Plan> arrayList = new ArrayList<>();
//
//        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용
//        Cursor cursor = db.rawQuery("SELECT * FROM PLANS ORDER BY dateInMillis ASC, startAngle ASC;", null);
//        while (cursor.moveToNext()) {
//
//            Plan plan = new Plan();
//            plan.id = cursor.getInt(0);
//            plan.planName = cursor.getString(1);
//            plan.dateInMillis = cursor.getLong(2);
//            plan.startAngle = cursor.getFloat(3);
//            plan.endAngle = cursor.getFloat(4);
//            plan.color = cursor.getInt(5);
//            plan.percentageOfAchieve = cursor.getInt(6);
//            plan.repeatTerm = cursor.getInt(7);
//            plan.repeatGroupId = cursor.getString(8);
//
//            arrayList.add(plan);
//        }
//
//        return arrayList;
//    }
}
