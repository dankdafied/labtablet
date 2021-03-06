package pt.up.fe.beta.labtablet.db_handlers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;

import pt.up.fe.beta.R;
import pt.up.fe.beta.labtablet.models.DataItem;
import pt.up.fe.beta.labtablet.models.Descriptor;
import pt.up.fe.beta.labtablet.models.Dictionary;
import pt.up.fe.beta.labtablet.models.FavoriteItem;
import pt.up.fe.beta.labtablet.utils.Utils;

/**
 * Class to handle favorite management and access to the "DB"
 */
public class FavoriteMgr {

    /**
     * Returns the base config loaded from the application profile
     * @param mContext used to access the preference manager
     */
    public static ArrayList<Descriptor> getBaseDescriptors(Context mContext) {
        SharedPreferences settings = mContext.getSharedPreferences(
                mContext.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);

        if (!settings.contains(Utils.BASE_DESCRIPTORS_ENTRY)) {
            Toast.makeText(mContext, "No metadata was found. Default configuration loaded.", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
        return new Gson().fromJson(
                settings.getString(Utils.BASE_DESCRIPTORS_ENTRY, ""),
                Utils.ARRAY_DESCRIPTORS);
    }

    /**
     * Loads and returns a favorite with the specified title (or a new one if non-existing)
     * @param context used to access the preference manager
     * @param favoriteName entry
     */
    public static FavoriteItem getFavorite(Context context, String favoriteName) {

        SharedPreferences settings = context.getSharedPreferences(
                context.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);

        String jsonData = settings.getString(favoriteName, "");
        if (!jsonData.equals("") && !jsonData.equals("[]")) {
            return new Gson().fromJson(jsonData, FavoriteItem.class);
        }

        //Toast.makeText(context, "No entry for that favorite was found", Toast.LENGTH_SHORT).show();
        return new FavoriteItem("");
    }

    /**
     * Adds a new favorite record entry
     * @param context used to access the preference manager
     * @param favorite the object to register
     */
    public static void registerFavorite(Context context, FavoriteItem favorite) {


        SharedPreferences settings = context.getSharedPreferences(
                context.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);

        if (settings.contains(favorite.getTitle())) {
            Toast.makeText(context, "Tried to add an existing favorite. Operation cancelled", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(favorite.getTitle(), new Gson().toJson(favorite));
        editor.apply();
    }

    /**
     * Removes entry from the DB including the linked files (if any)
     * @param context used to access the preference manager
     * @param favorite the object to remove
     */
    public static void removeFavoriteEntry(Context context, FavoriteItem favorite, boolean removeData) {
        SharedPreferences settings = context.getSharedPreferences(
                context.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);

        if (!settings.contains(favorite.getTitle())) {
            Toast.makeText(context, "Entry " + favorite.getTitle() + " was not found...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!removeData) {
            SharedPreferences.Editor editor = settings.edit();
            editor.remove(favorite.getTitle());
            editor.apply();
            return;
        }

        //Delete linked data resources
        ArrayList<DataItem> favoriteDataResources = favorite.getDataItems();
        if (favoriteDataResources != null &&
                favoriteDataResources.size() > 0) {

            for (DataItem item : favoriteDataResources) {
                //Burn!!
                if (!new File(item.getLocalPath()).delete()) {
                    Log.e("DELETE", "Failed to delete" + item.getLocalPath());
                }
            }
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.remove(favorite.getTitle());
        editor.apply();
    }

    /**
     * Updates the selected favorite's attributes
     * @param entryName db entry
     * @param item the item to update
     * @param context used to access the preference manager
     */
    @SuppressLint("CommitPrefEdits")
    public static void updateFavoriteEntry(String entryName, FavoriteItem item, Context context) {
            SharedPreferences settings = context.getSharedPreferences(
                    context.getResources().getString(R.string.app_name),
                    Context.MODE_PRIVATE);

            if (!settings.contains(entryName)) {
                Log.e("OVERWRITE", "Entry was not found for folder " + entryName);
                return;
            }

            SharedPreferences.Editor editor = settings.edit();
            editor.remove(entryName);
            editor.putString(entryName, new Gson().toJson(item, FavoriteItem.class));
            editor.commit();
    }

    /**
     * Updates the application dictionary (set of closed vocabulary entries, global to the app)
     * @param dictionary the input dictionary
     * @param context activity or context to access SharedPreferences object
     */
    public static void updateApplicationDictionary(Dictionary dictionary, Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                context.getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);

        if (settings.contains(Utils.DICTIONARY_ENTRY)) {
            Log.i("OVERWRITE", "Dictionary entry will be updated");
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.remove(Utils.DICTIONARY_ENTRY);
        editor.putString(Utils.DICTIONARY_ENTRY, new Gson().toJson(dictionary, Dictionary.class));
        editor.apply();
    }
}
