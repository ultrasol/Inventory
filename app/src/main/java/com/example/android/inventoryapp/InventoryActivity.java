package com.example.android.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

/**
 * Displays list of products that were entered and stored in the app.
 */
public class InventoryActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Identifier for the product data loader.
     */
    private static final int PRODUCT_LOADER = 0;

    /**
     * Adapter for the ListView.
     */
    ProductCursorAdapter mCursorAdapter;

    public static Resources sResources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        sResources = getResources();

        /* Setup FAB to open EditorActivity. */
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        /* Find the ListView which will be populated with the product data. */
        ListView productListView = (ListView) findViewById(R.id.list);

        /* Find and set empty view on the ListView, so that it only shows when the list has 0 items. */
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        /*
            Setup an Adapter to create a list item for each row of product data in the Cursor.
            There is no product data yet (until the loader finishes) so pass in null for the Cursor.
         */
        mCursorAdapter = new ProductCursorAdapter(this, null);
        productListView.setAdapter(mCursorAdapter);

        /* Setup the item click listener. */
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                /* Create new intent to go to {@link EditorActivity}. */
                Intent intent = new Intent(InventoryActivity.this, DetailActivity.class);

                /*
                    Form the content URI that represents the specific product that was clicked on,
                    by appending the "id" (passed as input to this method) onto the
                    {@link ProductEntry#CONTENT_URI}.
                    For example, the URI would be "content://com.example.android.inventoryapp/products/2"
                    if the product with ID 2 was clicked on.
                 */
                Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);

                /* Set the URI on the data field of the intent. */
                intent.setData(currentProductUri);

                /* Launch the {@link EditorActivity} to display the data for the current product. */
                startActivity(intent);
            }
        });

        /* Kick off the loader. */
        getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    /**
     * Helper method to delete all products in the database.
     */
    private void deleteAllProducts() {
        int rowsDeleted = getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);
        Log.v("InventoryActivity", rowsDeleted + " rows deleted from product database.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*
            Inflate the menu options from the res/menu/menu_inventory.xml file.
            This adds menu items to the app bar.
         */
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* User clicked on a menu option in the app bar overflow menu. */
        switch (item.getItemId()) {
            /* Respond to a click on the "Delete all entries" menu option. */
            case R.id.action_delete_all_entries:
                deleteAllProducts();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        /* Define a projection that specifies the columns from the table we care about. */
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_PICTURE
        };

        /* This loader will execute the ContentProvider's query method on a background thread. */
        return new CursorLoader(
                this,                       // Parent activity context.
                ProductEntry.CONTENT_URI,   // Provider content URI to query.
                projection,                 // Columns to include in the resulting Cursor.
                null,                       // No selection clause.
                null,                       // No selection arguments.
                null                        // Default sort order.
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* Update {@link ProductCursorAdapter} with this new cursor containing updated product data. */
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /* Callback called when the data needs to be deleted. */
        mCursorAdapter.swapCursor(null);
    }
}
