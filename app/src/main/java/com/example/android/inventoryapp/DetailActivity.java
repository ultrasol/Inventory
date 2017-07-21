package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Identifier for the product data loader.
     */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    private static final int RESULT_LOAD_IMAGE = 1;

    /**
     * Content URI for the existing product.
     */
    private Uri mCurrentProductUri;

    private TextView mNameTextView;
    private TextView mQuantityTextView;
    private TextView mPriceTextView;
    private ImageView mPictureImageView;

    private String mNameString;
    private String mQuantityString;
    private String mPriceString;
    private byte[] mPictureBytes;

    private int mQuantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        /* This is an existing product, so change app bar to say "Detail Product". */
        setTitle(getString(R.string.editor_activity_title_detail_product));

        mNameTextView = (TextView) findViewById(R.id.detail_name);
        mQuantityTextView = (TextView) findViewById(R.id.detail_quantity);
        mPriceTextView = (TextView) findViewById(R.id.detail_price);
        mPictureImageView = (ImageView) findViewById(R.id.detail_picture);

        Button decreaseButton = (Button) findViewById(R.id.detail_decrease_quantity);
        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mQuantity != 0) {
                    mQuantity--;
                    mQuantityTextView.setText(String.format("%1$s", Integer.toString(mQuantity)));
                }
            }
        });

        Button increaseButton = (Button) findViewById(R.id.detail_increase_quantity);
        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuantity++;
                mQuantityTextView.setText(String.format("%1$s", Integer.toString(mQuantity)));
            }
        });

        Button orderButton = (Button) findViewById(R.id.detail_order_product);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); /* Only email apps should handle this. */
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.supplier_email) });
                intent.putExtra(Intent.EXTRA_SUBJECT, mNameString);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        Button deleteButton = (Button) findViewById(R.id.detail_delete_product);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showDeleteConfirmationDialog(); }
        });

        Button loadPictureButton = (Button) findViewById(R.id.detail_load_picture);
        loadPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_OPEN_DOCUMENT,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                );
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        /*
            Initialize a loader to read the product data from the database
            and display the current values in the editor.
         */
        getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
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
     * Save product into database.
     */
    private void saveProduct() {
        String quantityString = mQuantityTextView.getText().toString().trim();

        Bitmap bitmap = ((BitmapDrawable) mPictureImageView.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] pictureBytes = baos.toByteArray();

        /*
            Create a ContentValues object where column names are the keys,
            and product attributes from the editor are the values.
         */
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);
        values.put(ProductEntry.COLUMN_PRODUCT_PICTURE, pictureBytes);

        /*
            This is an EXISTING product,
            so update the product with content URI mCurrentPetUri and pass in the new ContentValues.
            Pass in null for the selection and selection args
            because mCurrentPetUri will already identify
            the correct row in the database that we want to modify.
         */
        int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

        /* Show a toast message depending on whether or not the update was successful. */
        if (rowsAffected == 0) {
            /* If no rows were affected, then there was an error with the update. */
            Toast.makeText(
                    this,
                    getString(R.string.editor_update_product_failed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            /* Otherwise, the update was successful and we can display a toast. */
            Toast.makeText(
                    this,
                    getString(R.string.editor_update_product_successful),
                    Toast.LENGTH_SHORT
            ).show();
        }
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
                saveProduct();

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
                mCurrentProductUri,     // Query the content URI for the current product.
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
            int pictureColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PICTURE);

            /* Extract out the value from the Cursor for the given column index. */
            mNameString = cursor.getString(nameColumnIndex);
            mQuantityString = cursor.getString(quantityColumnIndex);
            mPriceString = cursor.getString(priceColumnIndex);
            mPictureBytes = cursor.getBlob(pictureColumnIndex);

            mQuantity = Integer.parseInt(mQuantityString);

            /* Update the views on the screen with the values from the database. */
            mNameTextView.setText(mNameString);
            mQuantityTextView.setText(mQuantityString);
            mPriceTextView.setText(getString(R.string.product_price, mPriceString));
            mPictureImageView.setImageBitmap(
                    BitmapFactory.decodeByteArray(
                            mPictureBytes,
                            0,
                            mPictureBytes.length
                    )
            );
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /* If the loader is invalidated, clear out all the data from the input fields. */
        mNameTextView.setText("");
        mQuantityTextView.setText("");
        mPriceTextView.setText("");
        mPictureImageView.setImageBitmap(null);
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        /*
            Create an AlertDialog.Builder and set the message, and click listeners
            for the positive and negative buttons on the dialog.
         */
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                /* User clicked the "Delete" button, so delete the product. */
                deleteProduct();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);

        /* Create and show the AlertDialog. */
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        /* Only perform the delete if this is an existing product. */
        if (mCurrentProductUri != null) {
            /*
                Call the ContentResolver to delete the product at the given content URI.
                Pass in null for the selection and selection args because the mCurrentProductUri
                content URI already identifies the product that we want.
             */
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            /* Show a toast message depending on whether or not the delete was successful. */
            if (rowsDeleted == 0) {
                /* If no rows were deleted, then there was an error with the delete. */
                Toast.makeText(
                        this,
                        getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                /* Otherwise, the delete was successful and we can display a toast. */
                Toast.makeText(
                        this,
                        getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        /* Close the activity. */
        finish();
    }
}