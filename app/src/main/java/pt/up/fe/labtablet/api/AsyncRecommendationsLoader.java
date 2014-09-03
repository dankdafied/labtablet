package pt.up.fe.labtablet.api;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;

import pt.up.fe.labtablet.models.Dendro.DendroConfiguration;
import pt.up.fe.labtablet.models.Dendro.DendroDescriptor;
import pt.up.fe.labtablet.models.Descriptor;
import pt.up.fe.labtablet.utils.FileMgr;
import pt.up.fe.labtablet.utils.Utils;

/**
 * Created by ricardo on 20-05-2014.
 */
public class AsyncRecommendationsLoader extends AsyncTask<Object, Integer, ArrayList<Descriptor>> {

    private AsyncTaskHandler<ArrayList<Descriptor>> mHandler;
    private Exception error;
    private String projectName;
    private Context mContext;
    private HttpGet httpget;

    public AsyncRecommendationsLoader(AsyncTaskHandler<ArrayList<Descriptor>> mHandler) {
        this.mHandler = mHandler;
    }


    @Override
    protected ArrayList<Descriptor> doInBackground(Object... params) {

        if(params[0] == null || params[1] == null) {
            error = new Exception("Expected Context, String, String; Got nulls");
            return new ArrayList<Descriptor>();
        }

        if( !(params[0] instanceof Context &&
                params[1] instanceof String)) {
            error = new Exception("Expected Context, String, String; Got "
                    + params[0].getClass() + ", " +
                    params[1].getClass());

            return new ArrayList<Descriptor>();
        }

        mContext = (Context) params[0];
        projectName = (String) params[1];


        try {
            String cookie = DendroAPI.authenticate(mContext);

            DendroConfiguration conf = FileMgr.getDendroConf(mContext);
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpget = new HttpGet(conf.getAddress() + "/project/" + projectName + "?metadata_recommendations");
            httpget.setHeader("Accept", "application/json");
            httpget.setHeader("Cookie", "connect.sid=" + cookie);

            HttpResponse resp = httpclient.execute(httpget);
            HttpEntity ent = resp.getEntity();

            JSONObject respObject = new JSONObject(EntityUtils.toString(ent));
            ArrayList<DendroDescriptor> recommendedDendroDescriptors =
                    new Gson().fromJson(respObject.get("descriptors").toString(), Utils.ARRAY_DENDRO_DESCRIPTORS);

            ArrayList<Descriptor> recommendedDescriptors = new ArrayList<Descriptor>();
            for (DendroDescriptor dDesc : recommendedDendroDescriptors) {
                Descriptor desc = new Descriptor();
                desc.setDescriptor(dDesc.getUri());
                desc.setName(dDesc.getShortName());
                desc.setDescription(dDesc.getComment());
                desc.setTag("");
                desc.setValue("");

                recommendedDescriptors.add(desc);
            }

            return recommendedDescriptors;

        } catch (Exception e) {
            error = e;
            return new ArrayList<Descriptor>();
        }
    }



    @Override
    protected void onPostExecute(ArrayList<Descriptor> result) {
        super.onPostExecute(result);
        if(error != null) {
            mHandler.onFailure(error);
        } else {
            mHandler.onSuccess(result);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        mHandler.onProgressUpdate(values[0]);
    }
}