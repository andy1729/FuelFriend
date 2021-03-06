package io.uscool.fuelfriend.Data;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.uscool.fuelfriend.R;
import io.uscool.fuelfriend.model.FuelPrice;
import io.uscool.fuelfriend.model.JsonAttributes;
import io.uscool.fuelfriend.model.State;
import io.uscool.fuelfriend.model.Town;

/**
 * Created by ujjawal on 4/7/17.
 * Helper class to store and retrieve data from database
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabasHelper";
    private static final String DB_NAME = "fuelFriend";
    private static final String DB_SUFFIX = ".db";
    private static final int DB_VERSION = 1;

    private static DatabaseHelper mInstance;
    private final Resources mResources;

    private static List<State> mStateList;
    private static List<Town> mTownList;

    private DatabaseHelper(Context context) {
        super(context, DB_NAME+DB_SUFFIX, null, DB_VERSION);
        mResources = context.getResources();
    }

    /**
     * Database access point
     * Singleton instance
     * @param context context of the activity
     * @return database instance
     */
    public static DatabaseHelper getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    /**
     * function to retrieve list of states from state table
     * @param context context of this is running in
     * @param fromDatabase <code>true</code> if a data refresh is needed, else <code>false</code>
     * @return All states stored in database
     */
    public static List<State> getStates(Context context, boolean fromDatabase) {
        if(mStateList != null || fromDatabase) {
            mStateList = loadStates(context);
        }
        return mStateList;
    }

    /**
     * function to retrieve list of towns from state table
     * @param context context of this is running in
     * @param fromDatabase <code>true</code> if a data refresh is needed, else <code>false</code>
     * @return All towns stored in database
     */
    public static List<Town> getTowns(Context context, boolean fromDatabase) {
        if(mTownList != null || fromDatabase) {
            mTownList = loadTowns(context);
        }
        return mTownList;
    }

    /**
     * use this function to get the Fuel price of current day
     * @param context context of this is running in
     * @param towncode towncode of town you want to get fuel price
     * @param isDiesel <code>true</code> if you want to know diesel price else <code>false</code>
     *                for petrol price
     * @param columnName name of column from which you want to retrieve data
     *                    pass null to get the data from column containing price of current day.
     * @return <code>isDiesel?currentDieselPrice:currentPetrolPrice</code>
     */
    public static String getCurrentFuelPriceForGiven(Context context, String towncode, boolean isDiesel,
                                                     String columnName) {
        if(columnName == null) {
            columnName = getCurrentDay();
        }
        SQLiteDatabase db = getReadableDatabase(context);
        String selectionArgs[] = {towncode};
        final String TABLE_NAME = isDiesel?HpclDieselPriceTable.NAME:HpclPetrolPriceTable.NAME;
        final String[] PROJECTION = {PriceBaseTable.COLUMN_TOWN_CODE, columnName};
        Cursor data = db.query(TABLE_NAME, PROJECTION, PriceBaseTable.COLUMN_TOWN_CODE
                + "=?", selectionArgs, null, null, null);
        if(data != null) {
            data.moveToFirst();
            String price = data.getString(1);
            data.close();
            return price; // magic number based on table projection
        }
        return null;
    }

    private static String getCurrentDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        Date d = new Date();
        return sdf.format(d);
    }

    private static List<State> loadStates(Context context) {
        Cursor data = getStateCursor(context);
        List<State> tmpStateList = new ArrayList<>(data.getCount());
        do {
            final State state = getState(data);
            tmpStateList.add(state);
        } while (data.moveToNext());
        return tmpStateList;
    }

    private static List<Town> loadTowns(Context context) {
        Cursor data = getTownCursor(context);
        List<Town> tmpTownList = new ArrayList<>(data.getCount());
        do {
            final Town town = getTown(data);
            tmpTownList.add(town);
        } while (data.moveToNext());
        return  tmpTownList;
    }

    /**
     * Gets all states wrapped in a {@link Cursor} positioned at it's first element.
     *
     * @param context The context this is running in.
     * @return All states stored in the database.
     */
    private static Cursor getStateCursor(Context context) {
        SQLiteDatabase database = getReadableDatabase(context);
        Cursor data = database.query(StateTable.NAME,
                StateTable.PROJECTION, null, null,
                null, null, null);
        data.moveToFirst();
        return data;
    }

    /**
     * Gets all towns wrapped in a {@link Cursor} positioned at it's first element.
     *
     * @param context The context this is running in.
     * @return All towns stored in the database.
     */
    private static Cursor getTownCursor(Context context) {
        SQLiteDatabase database = getReadableDatabase(context);
        Cursor data = database.query(TownTable.NAME, TownTable.PROJECTION, null,
                null, null, null, null);
        data.moveToFirst();
        return data;
    }

    /**
     * Gets a state from the given position of the cursor provided.
     *
     * @param data The Cursor containing the data.
     * @return The found category.
     */
    private static State getState(Cursor data) {
        // magic number based on StateTable projection
        final String name = data.getString(1);
        final String code = data.getString(2);

        return new State(name, code);
    }
    /**
     * Gets a town from the given position of the cursor provided.
     *
     * @param data The Cursor containing the data.
     * @return The found category.
     */
    private static Town getTown(Cursor data) {
        // magic number based on TownTable projection
        final String name = data.getString(1);
        final String code = data.getString(2);
        final String stateCode = data.getString(3);
        final String latitude = data.getString(4);
        final String longitude = data.getString(5);
        final boolean is_metro = (data.getString(6).equals("Y"));
        return new Town(name, code, stateCode, latitude, longitude, is_metro);
    }

    /**
     * Updates the Fuel price table in the database
     * @param context context of this is running in
     * @param fuelPriceList List containing {@link FuelPrice}
     */
    public static void updateFuelPrice(Context context, List<FuelPrice> fuelPriceList) {
        SQLiteDatabase writableDatabase = getWritableDatabase(context);
        for(FuelPrice fuelPrice: fuelPriceList ) {
            updatePriceInTable(writableDatabase, HpclDieselPriceTable.NAME, fuelPrice.getTownCode(),
                    fuelPrice.getDieselPrice());
            updatePriceInTable(writableDatabase, HpclPetrolPriceTable.NAME, fuelPrice.getTownCode(),
                    fuelPrice.getPetrolPrice());
        }
    }

    private static void updatePriceInTable(SQLiteDatabase writableDatabase, final String TABLE_NAME, String townCode, String price) {
        final String COLUMN_NAME = getCurrentDay();
        ContentValues priceValues = createContentValuesFor(COLUMN_NAME, price);
        createColumnInTable(writableDatabase, TABLE_NAME, COLUMN_NAME);
        writableDatabase.update(TABLE_NAME, priceValues, HpclDieselPriceTable.COLUMN_TOWN_CODE
                + "=?", new String[]{townCode});

    }

    private static void createColumnInTable(SQLiteDatabase db, final String TABLE_NAME, final String COLUMN_NAME) {
        if(!(existsColumnInTable(db, TABLE_NAME, COLUMN_NAME))) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_NAME + " TEXT");
        }
    }
    /**
     * Creates the content values to update Fuel Price(petrol and diesel both) in the database.
     *
     * @param fuelPrice the price of fuel
     * @return ContentValues containing updatable data.
     */
    private static ContentValues createContentValuesFor(String columnName, String fuelPrice) {
        ContentValues values = new ContentValues();
        values.clear();
        values.put(columnName, fuelPrice);
        return values;
    }


    private static SQLiteDatabase getReadableDatabase(Context context) {
        return getInstance(context).getReadableDatabase();
    }

    private static SQLiteDatabase getWritableDatabase(Context context) {
        return getInstance(context).getWritableDatabase();
    }

    private static boolean existsColumnInTable(SQLiteDatabase inDatabase, String inTable, String columnToCheck) {
        Cursor mCursor = null;
        try {
            // Query 1 row
            mCursor = inDatabase.rawQuery("SELECT " + columnToCheck + " FROM " + inTable, null);

            // getColumnIndex() gives us the index (0 to ...) of the column - otherwise we get a -1
            return  true;

        } catch (Exception Exp) {
            // Something went wrong. Missing the database? The table?
            Log.d("existsColumnInTable", "When checking whether a column exists in the table, an error occurred: " + Exp.getMessage());
            return false;
        } finally {
            if (mCursor != null) mCursor.close();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create the state table first followed by TownTable and others
        // because of the Foreign key dependency
        db.execSQL(StateTable.CREATE);
        db.execSQL(TownTable.CREATE);
        db.execSQL(HpclDieselPriceTable.CREATE);
        db.execSQL(HpclPetrolPriceTable.CREATE);
        preFillDatabase(db);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // leaving it blank for now
    }

    private void preFillDatabase(SQLiteDatabase db) {
        try {
            db.beginTransaction();
            try {
                preFillStatesAndTowns(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "preFillDatabase", e);
        }
    }

    private void preFillStatesAndTowns(SQLiteDatabase db) throws IOException, JSONException {
        ContentValues values = new ContentValues(); // reduce , reuse
        JSONArray jsonArray = new JSONArray(readStatesFromResources());
        JSONObject state;
        for(int i = 0; i < jsonArray.length(); i++) {
            state = jsonArray.getJSONObject(i);
            final String stateCode = state.getString(JsonAttributes.STATE_CODE);
            fillStates(db, values, state, stateCode);
            final JSONArray towns = state.getJSONArray(JsonAttributes.TOWNS);
            fillTownsForState(db, values, towns, stateCode);
        }
    }

    private String readStatesFromResources() throws IOException {
        StringBuilder statesJson = new StringBuilder();
        InputStream rawStates = mResources.openRawResource(R.raw.town);
        BufferedReader reader = new BufferedReader(new InputStreamReader(rawStates));
        String line;
        while ((line = reader.readLine()) != null) {
            statesJson.append(line);
        }

        return statesJson.toString();
    }

    private void fillStates(SQLiteDatabase db, ContentValues values, JSONObject state,
                            String stateCode) throws JSONException {
        values.clear();
        values.put(StateTable.COLUMN_NAME, state.getString(JsonAttributes.STATE_NAME));
        values.put(StateTable.COLUMN_CODE, stateCode);
        db.insert(StateTable.NAME, null, values);
    }

    private void fillTownsForState(SQLiteDatabase db, ContentValues values,
                                   JSONArray towns, String stateCode) throws JSONException {
        JSONObject town;
        for(int i = 0; i < towns.length(); i++) {
            town = towns.getJSONObject(i);
            values.clear();
            String townCode =  town.getString(JsonAttributes.TOWN_CODE);
            String townName = town.getString(JsonAttributes.TOWN_NAME);
            values.put(TownTable.COLUMN_CODE,townCode);
            values.put(TownTable.COLUMN_NAME, townName);
            values.put(TownTable.COLUMN_STATE_ID, stateCode);
            values.put(TownTable.COLUMN_LATITUDE, town.getString(JsonAttributes.TOWN_LATITUDE));
            values.put(TownTable.COLUMN_LONGITUDE, town.getString(JsonAttributes.TOWN_LONGITUDE));
            values.put(TownTable.COLUMN_IS_METRO, town.getString(JsonAttributes.TOWN_IS_METRO));
            db.insert(TownTable.NAME, null, values);
            fillHpclDieselAndPetrolForTown(db, values, townCode, townName);
        }
    }

    private void fillHpclDieselAndPetrolForTown(SQLiteDatabase db, ContentValues values,
                                                String townCode, String townName) {
        values.clear();
        values.put(HpclDieselPriceTable.COLUMN_TOWN_CODE, townCode);
        values.put(HpclDieselPriceTable.COLUMN_TOWN_NAME, townName);
        db.insert(HpclDieselPriceTable.NAME, null, values);
        db.insert(HpclPetrolPriceTable.NAME, null, values);
    }


      /*
    * Helper function for DatabaseManager
    * Remember to remove it in before going into production
    * */

    public ArrayList<Cursor> getData(String Query){
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[] { "message" };
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<>(2);
        MatrixCursor Cursor2= new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);

        try{
//            String maxQuery = Query ;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(Query, null);

            //add value to cursor2
            Cursor2.addRow(new Object[] { "Success" });

            alc.set(1,Cursor2);
            if (null != c && c.getCount() > 0) {

                alc.set(0,c);
                c.moveToFirst();

                return alc ;
            }
            return alc;
        } catch(SQLException sqlEx){
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        } catch(Exception ex){
            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+ex.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        }
    }

}
