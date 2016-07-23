package com.sam_chordas.android.stockhawk.ui;


import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.util.ArrayList;
import java.util.Locale;

/* Open an activity to draw a line chart depicting the history of the quote selected */
public class StockDetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int DEFAULT_RANGE_WEEK = 7;
    private static final int RANGE_MONTH = 30;
    private static String symbol;
    private static String name;
    private static String range;
    private final String LOG_TAG = StockDetailsActivity.class.getSimpleName();
    boolean isConnected;
    private Intent mServiceIntent;
    private Cursor mCursor;
    private LineChart lineChartView;
    private ProgressWheel progressView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context mContext = this;
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        setContentView(R.layout.activity_line_graph);
        TextView err_txt = (TextView) findViewById(R.id.emptyview);
        lineChartView = (LineChart) findViewById(R.id.linechart);
        progressView = (ProgressWheel) findViewById(R.id.progress_wheel);
        Bundle extras = getIntent().getExtras();
        symbol = extras.getString(MyStocksActivity.EXTRA_SYMBOL);
        name = extras.getString(MyStocksActivity.EXTRA_NAME);
        range = getString(R.string.week);
        if (name != null && !name.isEmpty()) {
            ActionBar mActionBar = getSupportActionBar();
            if (mActionBar != null) {
                mActionBar.setTitle(name);
            }
            mServiceIntent = new Intent(this, StockIntentService.class);
            if (savedInstanceState == null) {
                if (isConnected) {
                    lineChartView.setNoDataText("");
                    progressView.setVisibility(View.VISIBLE);
                    range = getString(R.string.week);
                    getHistoricData(DEFAULT_RANGE_WEEK);
                } else {
                    if (err_txt != null) {
                        err_txt.setVisibility(View.VISIBLE);
                    }
                }
            }
            getLoaderManager().initLoader(MyStocksActivity.CURSOR_LOADER_ID, null, this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_stocks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_week_history: {
                if (!range.equals(getString(R.string.week))) {
                    getHistoricData(DEFAULT_RANGE_WEEK);
                    range = getString(R.string.week);
                }
                break;
            }
            case R.id.action_month_history: {
                if (!range.equals(getString(R.string.month))) {
                    getHistoricData(RANGE_MONTH);
                    range = getString(R.string.month);
                }
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    public void getHistoricData(int daysSinceToday) {
        // Add the history of stock to DB
        mServiceIntent.putExtra("tag", "history");
        mServiceIntent.putExtra("symbol", symbol);
        mServiceIntent.putExtra("range", daysSinceToday);
        startService(mServiceIntent);
    }

    private void drawLineChart() {
        mCursor.moveToFirst();
        ArrayList<Entry> entries = new ArrayList<>();
        // creating labels
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < mCursor.getCount(); i++) {
            String dateStr = mCursor.getString(mCursor.getColumnIndex(HistoryColumns.DATE));
            float price = Float.parseFloat(mCursor.getString(mCursor.getColumnIndex(HistoryColumns.VALUE)));
            entries.add(new Entry(price, i));
            labels.add(dateStr.substring(5, 10));
            mCursor.moveToNext();
        }
//        LineDataSet dataset = new LineDataSet(entries, "previous " + range);
        LineDataSet dataset = new LineDataSet(entries, getString(R.string.dataset_description,range));
        dataset.setColors(ColorTemplate.COLORFUL_COLORS);
        dataset.setLineWidth(3);
        dataset.setDrawValues(false);
        LineData data = new LineData(labels, dataset);
        lineChartView.setDescription(symbol);  // set the description
        lineChartView.setDescriptionTextSize(12f);
        lineChartView.animateX(2000);
        lineChartView.getXAxis().setTextSize(12f);
        progressView.setVisibility(View.GONE);
        lineChartView.setData(data); // set the data and list of lables into chart
        //to set the zoom
//        lineChartView.setAutoScaleMinMaxEnabled(true);
//        lineChartView.getAxisLeft().setStartAtZero(false);
//        lineChartView.getAxisRight().setStartAtZero(false);
        lineChartView.invalidate();
        String desc = String.format(Locale.US, getString(R.string.chart_view_desc), name, range);
        lineChartView.setContentDescription(desc);
    }

    //Using the data from the database to load the line chart.
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.History.CONTENT_URI,
                null,
                QuoteColumns.SYMBOL + " = ?",
                new String[]{symbol},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() != 0) {
            mCursor = data;
            drawLineChart();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
