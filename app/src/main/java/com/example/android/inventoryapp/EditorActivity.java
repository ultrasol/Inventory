package com.example.android.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Allows user to create a new product.
 */
public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int RESULT_LOAD_IMAGE = 1;

    /**
     * EditText field to enter the product's name.
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the product's quantity.
     */
    private EditText mQuantityEditText;

    /**
     * EditText field to enter the product's price.
     */
    private EditText mPriceEditText;

    private ImageView mPictureImageView;
    private Button mLoadPictureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        setTitle(getString(R.string.editor_activity_title_new_product));

        /* Find all relevant views that we will need to read user input from. */
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);

        mPictureImageView = (ImageView) findViewById(R.id.image_view_product);
        mLoadPictureButton = (Button) findViewById(R.id.button_load_picture);
        mLoadPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_OPEN_DOCUMENT,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                );
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                mPictureImageView.setImageBitmap(bitmap);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get user input from editor and save product into database.
     *
     * @return true if the new product has been successfully inserted, false otherwise.
     */
    private boolean saveProduct() {
        /* Read from input fields and use trim to eliminate leading or trailing white space. */
        String nameString = mNameEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();

        if (nameString.isEmpty() || quantityString.isEmpty() || priceString.isEmpty() || mPictureImageView.getDrawable() == null) {
            Toast.makeText(
                    this,
                    getString(R.string.editor_save_product_missing),
                    Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        Bitmap bitmap = ((BitmapDrawable) mPictureImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] pictureBytes = baos.toByteArray();

        /*
            Check if this is supposed to be a new product
            and check if all the fields in the editor are blank.
         */
        if (
                TextUtils.isEmpty(nameString)
                && TextUtils.isEmpty(quantityString)
                && TextUtils.isEmpty(priceString)
                && pictureBytes == null
        ) {
            /*
                Since no fields were modified, we can return early without creating a new product.
                No need to create ContentValues and no need to do any ContentProvider operations.
             */
            return true;
        }

        /*
            Create a ContentValues object where column names are the keys,
            and product attributes from the editor are the values.
         */
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceString);
        values.put(ProductEntry.COLUMN_PRODUCT_PICTURE, pictureBytes);

        /*
            This is a NEW product, so insert a new product into the provider,
            returning the content URI for the new product.
         */
        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

        /* Show a toast message depending on whether or not the insertion was successful. */
        if (newUri == null) {
            /* If the new content URI is null, then there was an error with insertion. */
            Toast.makeText(
                    this,
                    getString(R.string.editor_insert_product_failed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            /* Otherwise, the insertion was successful and we can display a toast. */
            Toast.makeText(
                    this,
                    getString(R.string.editor_insert_product_successful),
                    Toast.LENGTH_SHORT
            ).show();
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*
            Inflate the menu options from the res/menu/menu.xml file.
            This adds menu items to the app bar.
         */
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* User clicked on a menu option in the app bar overflow menu. */
        switch (item.getItemId()) {
            /* Respond to a click on the "Save" menu option. */
            case R.id.action_save:
                /* Save product to database. */
                if (saveProduct())
                    /* Exit activity. */
                    finish();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        /*
            Since the editor shows all product attributes, define a projection that contains
            all columns from the product table.
         */
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_PICTURE
        };

        /* This loader will execute the ContentProvider's query method on a background thread. */
        return new CursorLoader(
                this,                   // Parent activity context.
                null,                   // Query the content URI for the current product.
                projection,             // Columns to include in the resulting Cursor.
                null,                   // No selection clause.
                null,                   // No selection arguments.
                null                    // Default sort order.
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        /* Bail early if the cursor is null or there is less than 1 row in the cursor. */
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        /*
            Proceed with moving to the first row of the cursor and reading data from it
            (this should be the only row in the cursor).
         */
        if (cursor.moveToFirst()) {
            /* Find the columns of product attributes that we're interested in. */
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int productColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PICTURE);

            /* Extract out the value from the Cursor for the given column index. */
            String name = cursor.getString(nameColumnIndex);
            String quantity = cursor.getString(quantityColumnIndex);
            String price = cursor.getString(priceColumnIndex);
            byte[] picture = cursor.getBlob(productColumnIndex);

            /* Update the views on the screen with the values from the database. */
            mNameEditText.setText(name);
            mQuantityEditText.setText(quantity);
            mPriceEditText.setText(price);
            mPictureImageView.setImageBitmap(
                    BitmapFactory.decodeByteArray(
                            picture,
                            0,
                            picture.length
                    )
            );
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /* If the loader is invalidated, clear out all the data from the input fields. */
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
        mPictureImageView.setImageBitmap(null);
    }
}