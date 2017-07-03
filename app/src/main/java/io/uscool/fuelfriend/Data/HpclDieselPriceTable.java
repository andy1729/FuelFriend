package io.uscool.fuelfriend.Data;

/**
 * Created by ujjawal on 3/7/17.
 *
 * Table interface to save HP Diesel Price
 * fetched from  http://hproroute.hpcl.co.in/StateDistrictMap_4/ms_hsd_price.jsp?param=T
 */

public interface HpclDieselPriceTable extends PriceBaseTable {
    String NAME = "HpclDieselPrice";

    String CREATE = "CREATE TABLE " + NAME + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY, "
            +  COLUMN_TOWN_CODE + " REFERENCES " + TownTable.NAME + "(" + TownTable.COLUMN_CODE + "), "
            + COLUMN_PRICE_MON + " TEXT, "
            + COLUMN_PRICE_TUES + " TEXT, "
            + COLUMN_PRICE_WED + " TEXT, "
            + COLUMN_PRICE_THU + " TEXT, "
            + COLUMN_PRICE_FRI + " TEXT, "
            + COLUMN_PRICE_SAT + " TEXT, "
            + COLUMN_PRICE_SUN + " TEXT);";
}
