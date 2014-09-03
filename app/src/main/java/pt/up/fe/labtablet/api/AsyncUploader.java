package pt.up.fe.labtablet.api;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.util.ArrayList;

import pt.up.fe.labtablet.R;
import pt.up.fe.labtablet.models.Dendro.DendroMetadataRecord;
import pt.up.fe.labtablet.models.Descriptor;
import pt.up.fe.labtablet.utils.FileMgr;
import pt.up.fe.labtablet.utils.Utils;
import pt.up.fe.labtablet.utils.Zipper;

public class AsyncUploader extends AsyncTask<Object, Integer, Void> {
    //input, remove, output
    private AsyncTaskHandler<Void> mHandler;
    private Exception error;
    private Context mContext;

    private String favoriteName;
    private String projectName;
    private String destUri;
    private String cookie;

    public AsyncUploader(AsyncTaskHandler<Void> mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mHandler.onProgressUpdate(values[0]);
    }


    @Override
    protected void onCancelled() {
        Log.e("","cancelled");
        super.onCancelled();
    }

    @Override
    protected Void doInBackground(Object... params) {

        if (params[0] instanceof String
                && params[1] instanceof String
                && params[2] instanceof String
                && params[3] instanceof Context) {

            favoriteName = (String) params[0];
            projectName = (String) params[1];
            destUri = (String) params[2];
            mContext = (Context) params[3];

            if (destUri.equals("")) {
                error = new Exception("Target Uri not defined!");
                return null;
            }

        } else {
            error = new Exception("Type mismatch");
            return null;
        }
        HttpClient httpclient;
        HttpPost httppost;

        publishProgress(10);
        destUri = destUri.replace(" ", "%20");

        //upload files (if any)
        String from = Environment.getExternalStorageDirectory() + "/" + mContext.getResources().getString(R.string.app_name) + "/" + favoriteName;

        //AUTHENTICATE USER
        publishProgress(20);
        try {
            cookie = DendroAPI.authenticate(mContext);
        } catch (Exception e) {
            error = e;
            return null;
        }

        if(new File(from + "/meta").exists() && new File(from + "/meta").listFiles().length > 0) {
            Zipper mZipper = new Zipper();
            String to = Environment.getExternalStorageDirectory() + "/" + favoriteName + ".zip";
            Log.i("ZIP_FROM", from);
            Log.i("ZIP_TO", to);
            Boolean result = mZipper.zipFileAtPath(from, to, mContext);

            if (!result) {
                Log.e("ZIP", "Failed to create zip file");
                error = new Exception("Failed to create zip file");
                return null;
            }

            publishProgress(25);

            httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);


            httppost = new HttpPost(destUri + "?restore");
            Log.d("[AsyncUploader] URI", destUri.replace(" ", "%20") + "?restore");
            httppost.setHeader("Cookie", "connect.sid=" + cookie);
            File file = new File(to);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            FileBody fileBody = new FileBody(file);
            builder.addPart("files[]", fileBody);
            builder.addTextBody("filename", favoriteName + ".zip");

            Log.d("[AsyncUploader]File Path", file.getAbsolutePath());

            httppost.setEntity(builder.build());
            Log.d("[AsyncUploader]POST", "" + httppost.getRequestLine());
            try {
                HttpResponse httpResponse = httpclient.execute(httppost);
                HttpEntity resEntity = httpResponse.getEntity();
                DendroResponse response = new Gson().fromJson(EntityUtils.toString(resEntity), DendroResponse.class);

                if (response.result.equals(Utils.DENDRO_RESPONSE_ERROR)) {
                    error = new Exception(response.result + ": " + response.message);
                    return null;
                }
                publishProgress(30);
                if(!file.delete()) {
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.deleting_temp_files), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("POST", e.getMessage());
                error = e;
            }
        }

        //export metadata
        publishProgress(50);
        ArrayList<Descriptor> descriptors = FileMgr.getDescriptors(favoriteName, mContext);
        ArrayList<DendroMetadataRecord> metadataRecords = new ArrayList<DendroMetadataRecord>();

        if (descriptors.size() == 0) {
            error = new Exception("No metadata found!");
            return null;
        }

        for (Descriptor descriptor : descriptors) {
            if (descriptor.hasFile()) {
                metadataRecords.add(new DendroMetadataRecord(
                        descriptor.getDescriptor(),
                        destUri + File.separator + "meta"
                                + File.separator +  descriptor.getValue()
                ));
            } else {
                metadataRecords.add(new DendroMetadataRecord(descriptor.getDescriptor(), descriptor.getValue()));
            }
        }

        //Post updated metadata to the repository
        publishProgress(70);
        try {
            httpclient = new DefaultHttpClient();
            httppost = new HttpPost(destUri + "?update_metadata");
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-Type", "application/json");
            httppost.setHeader("Cookie", "connect.sid=" + cookie);
            StringEntity se = new StringEntity(new Gson().toJson(metadataRecords, Utils.ARRAY_DENDRO_METADATA_RECORD), HTTP.UTF_8);
            Log.e("metadata", new Gson().toJson(metadataRecords, Utils.ARRAY_DENDRO_METADATA_RECORD));
            httppost.setEntity(se);

            HttpResponse resp = httpclient.execute(httppost);
            HttpEntity ent = resp.getEntity();
            DendroResponse metadataResponse = new Gson().fromJson(EntityUtils.toString(ent), DendroResponse.class);
            if (metadataResponse.result.equals(Utils.DENDRO_RESPONSE_ERROR) ||
                    metadataResponse.result.equals(Utils.DENDRO_RESPONSE_ERROR_2)) {
                error = new Exception(metadataResponse.result + ": " + metadataResponse.message);
                return null;
            }
            publishProgress(90);


            publishProgress(100);

            //finito
        } catch (Exception e) {
            error = e;
        }

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

    public class DendroResponse {
        public String result;
        public String message;
    }

}
