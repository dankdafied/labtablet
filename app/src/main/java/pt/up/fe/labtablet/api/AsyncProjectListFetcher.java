package pt.up.fe.labtablet.api;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import pt.up.fe.labtablet.models.Dendro.DendroConfiguration;
import pt.up.fe.labtablet.models.Dendro.ProjectListResponse;
import pt.up.fe.labtablet.utils.FileMgr;

/**
 * Created by ricardo on 08-05-2014.
 */
public class AsyncProjectListFetcher extends AsyncTask<Context, Integer, ProjectListResponse> {
    private AsyncTaskHandler<ProjectListResponse> mHandler;
    private Exception error;
    private Context mContext;
    HttpGet httpget;

    public AsyncProjectListFetcher(AsyncTaskHandler<ProjectListResponse> mHandler) {
        this.mHandler = mHandler;
    }

    @Override
    protected ProjectListResponse doInBackground(Context... params) {
        if(params[0] == null) {
            error = new Exception("Expected Context, got null");
            return null;
        }
        if( !(params[0] instanceof Context)) {
            error = new Exception("Type mismatch (expected Context)");
            return null;
        }
        mContext = (Context) params[0];

        try {
            String cookie = DendroAPI.authenticate(mContext);

            DendroConfiguration conf = FileMgr.getDendroConf(mContext);
            HttpClient httpclient = new DefaultHttpClient();
            httpget = new HttpGet(conf.getAddress() + "/projects/my");
            httpget.setHeader("Accept", "application/json");
            httpget.setHeader("Cookie", "connect.sid=" + cookie);

            HttpResponse resp = httpclient.execute(httpget);
            HttpEntity ent = resp.getEntity();
            ProjectListResponse listResponse = new Gson().fromJson(EntityUtils.toString(ent), ProjectListResponse.class);
            return listResponse;

        } catch (Exception e) {
            error = e;
            return null;
        }
    }


    @Override
    protected void onPostExecute(ProjectListResponse result) {
        super.onPostExecute(result);
        if (error != null) {
            mHandler.onFailure(error);
        } else {
            mHandler.onSuccess(result);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {

        mHandler.onProgressUpdate(values[0]);
        super.onProgressUpdate(values[0]);
    }
}
