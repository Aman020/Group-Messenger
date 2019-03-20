package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        //Log.i("Insert-", "Calling the insertInInternalStorage function");
        uri= insertInInternalStorage(uri, values, getContext());
        Log.v("insert", values.toString());
        return uri;
    }
    private Uri insertInInternalStorage(Uri uri, ContentValues contentValues, Context context)
    {
        FileOutputStream outputStream = null;
        try
        {
            String fileName = contentValues.getAsString("key");
            String value = contentValues.getAsString("value");
            if( (fileName != null) && (value != null)) {
                outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(value.getBytes());
                outputStream.flush();
                outputStream.close();
            }
            else return null;
        }
        catch(IOException io)
        {
            io.printStackTrace();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        finally {
//            outputStream.close();
        }

        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        try {
            Thread.sleep(500);
            Log.i("Query-", "Calling the getQueryResult()");
            Cursor returnResult = getQueryResult(uri, selection, getContext());
            Log.v("query", selection);
            return returnResult;
        }catch (Exception e)
        {
            return null;
        }
    }
    private  Cursor getQueryResult(Uri uri, String selectionClause, Context context)
    {
        // References -https://developer.android.com/reference/android/database/MatrixCursor - Used to learn the concept of matrixCursor. It offers a function addRow(object[] columns) to add the row.
        try {
            // Creating an object of MatrixCursor which is basically the implementation of the cursor we can use
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});
            // Creating a stream object. Various other alternate byte/character streams can be used to read and write the data.
            FileInputStream inputstream = context.openFileInput(selectionClause);
            int res =0;
            // creating an object of StringBuilder to efficiently append the data which is read using read() function. Read() function returns a byte and hence we use while loop to read all the bytes. It returns -1 if its empty.
            StringBuilder sb = new StringBuilder();
            while((res = inputstream.read())!= -1)
            {
                sb.append((char) res );

            }
            matrixCursor.addRow( new String[]{selectionClause, sb.toString()} );
            inputstream.close();
            return matrixCursor;
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
