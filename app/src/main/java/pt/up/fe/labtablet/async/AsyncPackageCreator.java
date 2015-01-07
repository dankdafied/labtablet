package pt.up.fe.labtablet.async;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import pt.up.fe.labtablet.R;
import pt.up.fe.labtablet.db_handlers.FavoriteMgr;
import pt.up.fe.labtablet.models.FavoriteItem;
import pt.up.fe.labtablet.models.Form;
import pt.up.fe.labtablet.models.ProgressUpdateItem;
import pt.up.fe.labtablet.utils.Zipper;

import static pt.up.fe.labtablet.utils.CSVHandler.generateCSV;

/**
 * Thread to prepare and zip a favorite and deposit the resulting file into the
 * device's root folder (data not root root)
 * After that, the async uploader can be called
 */
public class AsyncPackageCreator extends AsyncTask<Object, ProgressUpdateItem, Void> {


    private AsyncCustomTaskHandler<Void> mHandler;
    private Exception error;

    public AsyncPackageCreator(AsyncCustomTaskHandler<Void> mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    protected void onProgressUpdate(ProgressUpdateItem... values) {
        mHandler.onProgressUpdate(values[0]);
    }


    @Override
    protected void onCancelled() {
        Log.e("", "cancelled");
        super.onCancelled();
    }

    @Override
    protected Void doInBackground(Object... params) {
        Context mContext;
        String favoriteName;

        if (params[0] instanceof String
                && params[1] instanceof Context) {

            favoriteName = (String) params[0];
            mContext = (Context) params[1];

        } else {
            error = new Exception("Type mismatch");
            return null;
        }

        FavoriteItem item = FavoriteMgr.getFavorite(mContext, favoriteName);

        //Generate forms and forms' csv file
        if (item.getLinkedForms().size() > 0) {
            publishProgress(new ProgressUpdateItem(15, "Generating " + item.getLinkedForms().size() + " forms..."));
            HashMap<String, ArrayList<Form>> linkedForms = item.getLinkedForms();

            try {
                generateCSV(mContext, linkedForms, favoriteName);
            } catch (IOException e) {
                error = e;
                return null;
            }
        }

        String from = Environment.getExternalStorageDirectory() + "/" + mContext.getResources().getString(R.string.app_name) + "/" + favoriteName;
        String to = Environment.getExternalStorageDirectory() + "/" + favoriteName + ".zip";

        publishProgress(new ProgressUpdateItem(40, mContext.getResources().getString(R.string.upload_progress_creating_package)));
        if (new File(from).listFiles().length > 0) {
            Zipper mZipper = new Zipper();

            Log.i("ZIP_FROM", from);
            Log.i("ZIP_TO", to);
            Boolean result = mZipper.zipFileAtPath(from, to, mContext);

            if (!result) {
                Log.e("ZIP", "Failed to create zip file");
                error = new Exception("Failed to create zip file");
                return null;
            }
        }

        Log.e("ZIP", "COMPLETED");
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        if (error != null) {
            mHandler.onFailure(error);
        } else {
            mHandler.onSuccess(null);
        }
    }
}