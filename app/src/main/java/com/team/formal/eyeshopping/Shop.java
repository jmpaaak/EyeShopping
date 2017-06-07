package com.team.formal.eyeshopping;

/**
 * Created by Administrator on 2017-05-23.
 */

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.Date;

public class Shop implements Serializable {


    private Date lastBuildDate;
    private int total;
    private int start;
    private int display;
    private Object item;
    private String title;
    private String link;
    private String image;
    private int lprice;
    private int hprice;
    private String mallName;
    private int productId;
    private int productType;
    private Bitmap bmp;

    public Shop(Date lastBuildDate, int total, int start, int display, Object item, String title, String link,
                String image, int lprice, int hprice, String mallName, int productId, int productType) {
        super();
        this.lastBuildDate = lastBuildDate;
        this.total = total;
        this.start = start;
        this.display = display;
        this.item = item;
        this.title = title;
        this.link = link;
        this.image = image;
        this.lprice = lprice;
        this.hprice = hprice;
        this.mallName = mallName;
        this.productId = productId;
        this.productType = productType;
    }
    public Shop() {
        super();
    }
    public Date getLastBuildDate() {
        return lastBuildDate;
    }
    public void setLastBuildDate(Date lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
    }
    public int getTotal() {
        return total;
    }
    public void setTotal(int total) {
        this.total = total;
    }
    public int getStart() {
        return start;
    }
    public void setStart(int start) {
        this.start = start;
    }
    public int getDisplay() {
        return display;
    }
    public void setDisplay(int display) {
        this.display = display;
    }
    public Object getItem() {
        return item;
    }
    public void setItem(Object item) {
        this.item = item;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    public String getImage() {
        return image;
    }
    public void setThumbBmp(Bitmap image) {
        this.bmp = image;
    }
    public Bitmap getThumbBmp() {
        return bmp;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public int getLprice() {
        return lprice;
    }
    public void setLprice(int lprice) {
        this.lprice = lprice;
    }
    public int getHprice() {
        return hprice;
    }
    public void setHprice(int hprice) {
        this.hprice = hprice;
    }
    public String getMallName() {
        return mallName;
    }
    public void setMallName(String mallName) {
        this.mallName = mallName;
    }
    public int getProductId() {
        return productId;
    }
    public void setProductId(int productId) {
        this.productId = productId;
    }
    public int getProductType() {
        return productType;
    }
    public void setProductType(int productType) {
        this.productType = productType;
    }
    @Override
    public String toString() {
        return "Shop [lastBuildDate=" + lastBuildDate + ", total=" + total + ", start=" + start + ", display=" + display
                + ", item=" + item + ", title=" + title + ", link=" + link + ", image=" + image + ", lprice=" + lprice
                + ", hprice=" + hprice + ", mallName=" + mallName + ", productId=" + productId + ", productType="
                + productType + "]";
    }
}
