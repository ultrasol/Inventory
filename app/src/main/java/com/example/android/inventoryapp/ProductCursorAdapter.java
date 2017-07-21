package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import static com.example.android.inventoryapp.InventoryActivity.sResources;

/**
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
public class ProductCursorAdapter extends CursorAdapter {
    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context.
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) { super(context, c, 0 /* Flags */); }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context The app context.
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct position.
     * @param parent  The parent to which the new view is attached to.
     * @return The newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        /* Inflate a list item view using the layout specified in list_item.xml. */
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    The existing view, returned earlier by newView() method.
     * @param context The app context.
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, cursor.getPosition() + 1);

        /* Find individual views that we want to modify in the list item layout. */
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);

        /* Find the columns of product attributes that we're interested in. */
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);

        /* Read the product attributes from the Cursor for the current product. */
        String productName = cursor.getString(nameColumnIndex);
        String productQuantity = cursor.getString(quantityColumnIndex);
        String productPrice = cursor.getString(priceColumnIndex);

        /* Update the TextViews with the attributes for the current product. */
        nameTextView.setText(productName);
        quantityTextView.setText(productQuantity);
        priceTextView.setText(sResources.getString(R.string.product_price, productPrice));

        Button saleButton = (Button) view.findViewById(R.id.sale);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String quantityString = quantityTextView.getText().toString().trim();
                int quantity = Integer.parseInt(quantityString);

                if (quantity != 0) {
                    quantity--;
                    quantityTextView.setText(String.format("%1$s", Integer.toString(quantity)));
                    quantityString = quantityTextView.getText().toString().trim();

                    /*
                        Create a ContentValues object where column names are the keys,
                        and product attributes from the editor are the values.
                     */
                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);

                    /*
                        This is an EXISTING product,
                        so update the product with content URI mCurrentPetUri and pass in the new ContentValues.
                        Pass in null for the selection and selection args
                        because mCurrentPetUri will already identify
                        the correct row in the database that we want to modify.
                     */
                    context.getContentResolver().update(currentProductUri, values, null, null);
                }
            }
        });
    }
}