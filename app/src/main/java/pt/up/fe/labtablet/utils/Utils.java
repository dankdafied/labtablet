package pt.up.fe.labtablet.utils;

import android.location.Location;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import pt.up.fe.labtablet.models.AssociationItem;
import pt.up.fe.labtablet.models.ChangelogItem;
import pt.up.fe.labtablet.models.Dendro.DendroDescriptor;
import pt.up.fe.labtablet.models.Dendro.DendroFolderItem;
import pt.up.fe.labtablet.models.Dendro.DendroMetadataRecord;
import pt.up.fe.labtablet.models.Descriptor;

/**
 * Created by ricardo on 21-03-2014.
 */
public class Utils {

    public static Type ARRAY_ASSOCIATION_ITEM = new TypeToken<ArrayList<AssociationItem>>() {}.getType();
    public static Type ARRAY_CHANGELOG_ITEM = new TypeToken<ArrayList<ChangelogItem>>() {}.getType();
    public static Type ARRAY_DESCRIPTORS = new TypeToken<ArrayList<Descriptor>>() {}.getType();
    public static Type ARRAY_DIRECTORY_LISTING = new TypeToken<ArrayList<DendroFolderItem>>() {}.getType();
    public static Type ARRAY_DENDRO_METADATA_RECORD = new TypeToken<ArrayList<DendroMetadataRecord>>() {}.getType();
    public static Type ARRAY_DENDRO_DESCRIPTORS = new TypeToken<ArrayList<DendroDescriptor>>() {}.getType();

    public static final String AUDIO_TAGS = "audio";
    public static final String PICTURE_TAGS = "image";
    public static final String GEO_TAGS = "position";
    public static final String TEXT_TAGS = "text";
    public static final String MAGNETIC_TAGS = "magnetic";
    public static final String TEMP_TAGS = "temperature";
    public static final String TITLE_TAG = "title";
    public static final String DESCRIPTION_TAG = "description";
    public static final String GENERIC_TAG = "generic";

    //Extension used to state a certain file is a folder (when ls)
    public static final String DENDRO_FOLDER_EXTENSION = "folder";
    public static final String DENDRO_RESPONSE_ERROR = "error";
    public static final String DENDRO_RESPONSE_ERROR_2 = "Error";
    public static final String DENDRO_CONFS_ENTRY = "dendro_configurations";

    public static String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather?";
    public static String ASSOCIATIONS_CONFIG_ENTRY = "associations";
    public static String DESCRIPTORS_CONFIG_ENTRY = "base_descriptors";
    public static String CHANGELOG_CONFIG_ENTRY = "changelogs";
    public static long SAMPLE_MILLIS = 5000;

    public static final int DESCRIPTOR_STATE_VALIDATED = 1;
    public static final int DESCRIPTOR_STATE_NOT_VALIDATED = 0;

    // ---- Activities Results ---------

    //Select a descriptor and return it
    public static int DESCRIPTOR_GET = 0;

    //Select a descriptor, define its value and return it
    public static int DESCRIPTOR_DEFINE = 1;

    //When calling metadatavalidationActivity and in return geting an array with the
    //validated records;
    public static final int METADATA_VALIDATION = 2;

    //Pick an application profile to load
    public static final int PROFILE_PICK = 3;

    //Pick a descriptor and associate it with the extension
    public static final int DESCRIPTOR_ASSOCIATE = 4;

    //Capture image from camera
    public static final int CAMERA_INTENT_REQUEST = 5;

    //Launch sketchActivity
    public static final int SKETCH_INTENT_REQUEST = 6;

    //Upload proccess
    public static int SUBMISSION_VALIDATION = 7;

    //When the metadata edition changed the title
    public static final int RESULT_TITLE_CHANGED = 8;

    //public static String search_repo = "http://demo.ckan.org";
    public static String search_repo = "http://datahub.io";


    public static String getDate() {
        return  String.format("%tFT%<tRZ",new Date());
    }

    public static String getDate(long date) {
        return String.format("%tFT%<tRZ", date);
    }
}