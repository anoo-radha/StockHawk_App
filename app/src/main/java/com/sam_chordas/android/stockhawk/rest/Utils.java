package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    public static boolean showPercent = true;
    private static String LOG_TAG = Utils.class.getSimpleName();

    public static ArrayList<ContentProviderOperation> quoteListToHistoryContents(Context c, String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        c.getContentResolver().delete(
                QuoteProvider.History.CONTENT_URI,
                            null, null);
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count != 0) {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
                    if (resultsArray != null && resultsArray.length() != 0) {
                        String Date = resultsArray.getJSONObject(0).getString("Date");
                        String today_date = Utils.getFormattedDate(0);
                        String symbol_got = resultsArray.getJSONObject(0).getString("Symbol");
                        if (!(Date.equals(today_date))) {
//                        //adding today's stock from quote db
                            Cursor cursor = c.getContentResolver().query(
                                    QuoteProvider.Quotes.withSymbol(symbol_got),
                                    new String[]{QuoteColumns.BIDPRICE},
                                    QuoteColumns.ISCURRENT + "= ?",
                                    new String[]{"1"},
                                    null);
                            if ((cursor != null) && (cursor.getCount() != 0)) {
                                cursor.moveToFirst();
                                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                                        QuoteProvider.History.CONTENT_URI);
                                builder.withValue(HistoryColumns.SYMBOL, symbol_got);
                                float price = Float.parseFloat(cursor.getString(cursor.getColumnIndex(QuoteColumns.BIDPRICE)));
                                builder.withValue(HistoryColumns.VALUE, price);
                                builder.withValue(HistoryColumns.DATE, today_date);
                                batchOperations.add(builder.build());
                                cursor.close();
                            }
                        }
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildHistory(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed as: " + e);
        }
        return batchOperations;
    }

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(Context c, String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    //If user enters invalid stock , then a invalid toast is shown
                    if (jsonObject.getString("Bid").equals("null")) {
                        setStockStatus(c, c.getString(R.string.stock_status_invalid));

                    } else {
                        batchOperations.add(buildBatchOperation(jsonObject));
                    }
                }
                //If user doesnt enter stock name and presses ok, then a 'enter valid stock' toast is shown
                else if (count == 0) {
                    setStockStatus(c, c.getString(R.string.stock_status_unknown));
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format(Locale.US, "%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String dummy_percent_change = "+0.00%";
        String dummy_change = "+0.00";
        //To remove 'ul' received error
        if (change == null) {
            if (isPercentChange) change = dummy_percent_change;
            else change = dummy_change;

        }
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format(Locale.US, "%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildHistory(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.History.CONTENT_URI);
        try {
            builder.withValue(HistoryColumns.SYMBOL, jsonObject.getString("Symbol"));
            builder.withValue(HistoryColumns.VALUE, Float.parseFloat(jsonObject.getString("Close")));
            builder.withValue(HistoryColumns.DATE, jsonObject.getString("Date"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }


    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            Log.i(LOG_TAG, "percent_change " + jsonObject.getString("ChangeinPercent"));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    /**
     * @param c Context used to get the SharedPreferences
     * @return the stock status integer type
     */
    static public String getStockStatus(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        return sp.getString(c.getString(R.string.pref_stock_status_key), c.getString(R.string.stock_status_valid));
    }

    /**
     * Resets the stock status.
     *
     * @param c Context used to get the SharedPreferences
     */
    static public void setStockStatus(Context c, String status) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString(c.getString(R.string.pref_stock_status_key), status);
        spe.apply();
    }

    static public String getFormattedDate(int daysSinceToday) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysSinceToday);
        return String.format(Locale.US, "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DATE));
    }
}
