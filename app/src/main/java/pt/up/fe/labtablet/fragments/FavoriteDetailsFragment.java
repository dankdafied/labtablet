package pt.up.fe.labtablet.fragments;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;

import pt.up.fe.labtablet.R;
import pt.up.fe.labtablet.activities.DescriptorPickerActivity;
import pt.up.fe.labtablet.activities.FieldModeActivity;
import pt.up.fe.labtablet.activities.SubmissionValidationActivity;
import pt.up.fe.labtablet.activities.ValidateMetadataActivity;
import pt.up.fe.labtablet.adapters.DataListAdapter;
import pt.up.fe.labtablet.adapters.MetadataListAdapter;
import pt.up.fe.labtablet.api.ChangelogManager;
import pt.up.fe.labtablet.async.AsyncFileImporter;
import pt.up.fe.labtablet.async.AsyncTaskHandler;
import pt.up.fe.labtablet.models.ChangelogItem;
import pt.up.fe.labtablet.models.DataItem;
import pt.up.fe.labtablet.models.Descriptor;
import pt.up.fe.labtablet.utils.DBCon;
import pt.up.fe.labtablet.utils.FileMgr;
import pt.up.fe.labtablet.utils.Utils;

public class FavoriteDetailsFragment extends Fragment {

    private TextView tv_title;
    private TextView tv_description;
    private ImageButton bt_edit_view;

    //Buttons to switch between data and metadata views
    private Button bt_meta_view;
    private Button bt_data_view;

    private ListView lv_metadata;
    private boolean isMetadataVisible;
    private ArrayList<Descriptor> itemDescriptors;
    private ArrayList<DataItem> dataItems;
    private String favoriteName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorite_view,
                container, false);

        setHasOptionsMenu(true);

        Button bt_new_metadata = (Button) rootView.findViewById(R.id.bt_new_metadata);
        Button bt_fieldMode = (Button) rootView.findViewById(R.id.bt_field_mode);
        ImageButton bt_edit_title = (ImageButton) rootView.findViewById(R.id.favorite_view_edit_title);

        tv_title = (TextView) rootView.findViewById(R.id.tv_title);
        tv_description = (TextView) rootView.findViewById(R.id.tv_description);

        lv_metadata = (ListView) rootView.findViewById(R.id.lv_favorite_metadata);

        bt_meta_view = (Button) rootView.findViewById(R.id.tab_metadata);
        bt_data_view = (Button) rootView.findViewById(R.id.tab_data);
        bt_edit_view = (ImageButton) rootView.findViewById(R.id.bt_edit_metadata);

        bt_edit_title.setTag(Utils.TITLE_TAG);

        if (savedInstanceState != null) {
            favoriteName = savedInstanceState.getString("favorite_name");
            isMetadataVisible = savedInstanceState.getBoolean("metadata_visible");
        } else {
            favoriteName = this.getArguments().getString("favorite_name");
            isMetadataVisible = true;
        }

        tv_title.setText(favoriteName);

        ActionBar mActionBar = getActivity().getActionBar();
        if (mActionBar == null) {
            ChangelogItem item = new ChangelogItem();
            item.setMessage("FavoriteDetails" + "Couldn't get actionbar. Compatibility mode layout");
            item.setTitle(getResources().getString(R.string.developer_error));
            item.setDate(Utils.getDate());
            ChangelogManager.addLog(item, getActivity());
        } else {
            mActionBar.setSubtitle(favoriteName);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        itemDescriptors = DBCon.getDescriptors(favoriteName, getActivity());
        //dataItems = DBCon.getDataDescriptionItems(getActivity(), favoriteName);


        for (Descriptor desc : itemDescriptors) {
            if (desc.getDescriptor() == null)
                break;
            if (desc.getTag().equals(Utils.DESCRIPTION_TAG)) {
                tv_description.setText(desc.getValue());
            }
            if (desc.getTag().equals(Utils.TITLE_TAG)) {
                tv_title.setText(desc.getValue());
            }
        }

        if (isMetadataVisible) {
            loadMetadataView();
        } else {
            loadDataView();
        }


        dcClickListener mClickListener = new dcClickListener();
        bt_edit_title.setOnClickListener(mClickListener);

        bt_fieldMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FieldModeActivity.class);
                intent.putExtra("favorite_name", favoriteName);
                startActivity(intent);
            }
        });

        bt_new_metadata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isMetadataVisible) {
                    Toast.makeText(getActivity(), "Choose the file", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("file/*");
                    startActivityForResult(intent, Utils.PICK_FILE_INTENT);
                } else {
                    Intent myIntent = new Intent(getActivity(), DescriptorPickerActivity.class);
                    myIntent.putExtra("file_extension", "");
                    myIntent.putExtra("favoriteName", favoriteName);
                    myIntent.putExtra("returnMode", Utils.DESCRIPTOR_DEFINE);
                    startActivityForResult(myIntent, Utils.DESCRIPTOR_DEFINE);
                }
            }
        });

        bt_edit_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMetadataVisible) {
                    Intent myIntent = new Intent(getActivity(), ValidateMetadataActivity.class);
                    myIntent.putExtra("favorite_name", favoriteName);
                    myIntent.putExtra("descriptors", new Gson().toJson(itemDescriptors, Utils.ARRAY_DESCRIPTORS));
                    startActivityForResult(myIntent, Utils.METADATA_VALIDATION);
                }
            }
        });

        bt_data_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadDataView();
            }
        });
        bt_meta_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadMetadataView();
            }
        });

        return rootView;
    }

    private void loadMetadataView() {
        bt_data_view.setEnabled(true);
        bt_meta_view.setEnabled(false);
        isMetadataVisible = true;
        bt_edit_view.setVisibility(View.VISIBLE);

        lv_metadata.setDividerHeight(0);
        itemDescriptors = DBCon.getDescriptors(favoriteName, getActivity());

        MetadataListAdapter mMetadataAdapter = new MetadataListAdapter(getActivity(), itemDescriptors, favoriteName);
        lv_metadata.setAdapter(mMetadataAdapter);
    }

    private void loadDataView() {
        bt_data_view.setEnabled(false);
        bt_meta_view.setEnabled(true);
        bt_edit_view.setVisibility(View.INVISIBLE);

        isMetadataVisible = false;
        dataItems = DBCon.getDataDescriptionItems(getActivity(), favoriteName);

        DataListAdapter mDataAdapter = new DataListAdapter(getActivity(), dataItems, favoriteName);
        lv_metadata.setAdapter(mDataAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isMetadataVisible) {
            loadMetadataView();
        } else {
            loadDataView();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null)
            return;

        //Add single descriptor
        if (requestCode == Utils.DESCRIPTOR_DEFINE || requestCode == Utils.DESCRIPTOR_GET) {
            if (!data.getExtras().containsKey("descriptor"))
                return;

            String descriptorJson = data.getStringExtra("descriptor");
            Descriptor newDescriptor = new Gson().fromJson(descriptorJson, Descriptor.class);
            itemDescriptors.add(newDescriptor);
            DBCon.overwriteDescriptors(favoriteName, itemDescriptors, getActivity());

            this.onResume();

        } else if (requestCode == Utils.METADATA_VALIDATION) {
            if (!data.getExtras().containsKey("descriptors"))
                return;

            String descriptorsJson = data.getStringExtra("descriptors");
            itemDescriptors = new Gson().fromJson(descriptorsJson, Utils.ARRAY_DESCRIPTORS);
            DBCon.overwriteDescriptors(favoriteName, itemDescriptors, getActivity());

            this.onResume();

        } else if (requestCode == Utils.PICK_FILE_INTENT) {

            final Dialog dialog = new Dialog(getActivity());
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.import_file_dialog);
            dialog.setTitle(getResources().getString(R.string.importing_file));

            final EditText importDescription = (EditText) dialog.findViewById(R.id.import_file_description);
            final ProgressBar importProgress = (ProgressBar) dialog.findViewById(R.id.import_file_progress);
            final Button importSubmit = (Button) dialog.findViewById(R.id.import_file_submit);
            final TextView importHeader = (TextView) dialog.findViewById(R.id.import_file_header);

            importSubmit.setEnabled(false);
            dialog.show();

            new AsyncFileImporter(new AsyncTaskHandler<DataItem>() {
                @Override
                public void onSuccess(final DataItem result) {
                    importSubmit.setEnabled(true);
                    importSubmit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String value = importDescription.getText().toString();

                            if (!value.equals("")) {
                                ArrayList<Descriptor> itemLevelDescriptors = result.getFileLevelMetadata();
                                for (Descriptor desc : itemLevelDescriptors) {
                                    if (desc.getTag().equals(Utils.DESCRIPTION_TAG)) {
                                        desc.setValue(value);
                                    }
                                }
                            }

                            DBCon.addDataItem(getActivity(), result, favoriteName);
                            dialog.dismiss();
                            onResume();
                        }
                    });

                }

                @Override
                public void onFailure(Exception error) {
                    Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_LONG).show();
                    onResume();
                    dialog.dismiss();
                }

                @Override
                public void onProgressUpdate(int value) {
                    importProgress.setProgress(value);
                    importHeader.setText("" + value + "%");
                }
            }).execute(getActivity(), data, favoriteName);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("favorite_name", favoriteName);
        outState.putBoolean("metadata_visible", isMetadataVisible);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.favorite_view_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        if (item.getItemId() == R.id.action_favorite_upload) {
            SharedPreferences settings = getActivity().getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
            if (!settings.contains(Utils.DENDRO_CONFS_ENTRY)) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.dendro_confs_not_found_title))
                        .setMessage(getResources().getString(R.string.dendro_confs_not_found_message))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(getResources().getDrawable(R.drawable.ic_error))
                        .show();
                return super.onOptionsItemSelected(item);
            }

            Intent mIntent = new Intent(getActivity(), SubmissionValidationActivity.class);
            mIntent.putExtra("favorite_name", favoriteName);
            getActivity().startActivityForResult(mIntent, Utils.SUBMISSION_VALIDATION);

        } else if (item.getItemId() == R.id.action_favorite_delete) {
            //remove this favorite
            new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .setTitle(R.string.edit_metadata_item_delete)
                    .setMessage(R.string.form_really_delete_favorite)
                    .setPositiveButton(R.string.form_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FileMgr.removeFavorite(favoriteName, getActivity());

                            ChangelogItem log = new ChangelogItem();
                            log.setMessage(getResources().getString(R.string.log_favorite_removed) + favoriteName);
                            log.setDate(Utils.getDate());
                            log.setTitle(getResources().getString(R.string.log_removed));
                            ChangelogManager.addLog(log, getActivity());

                            FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
                            //getActivity().getFragmentManager().popBackStack();

                            //transaction.replace(R.id.frame_container, new ListFavoritesFragment());
                            getFragmentManager().popBackStack();

                            transaction.commit();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

        }
        return super.onOptionsItemSelected(item);

    }

    private class dcClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("New value");
            final View mView = view;

            // Set up the input
            final EditText input = new EditText(getActivity());
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            builder.setView(input);
            builder.setMessage(getResources().getString(R.string.update_name_instructions));

            if (mView.getTag().equals(Utils.TITLE_TAG)) {
                input.setText(favoriteName);
            } else {
                input.setText(tv_description.getText().toString());
            }

            // Set up the buttons
            builder.setPositiveButton(getResources().getString(R.string.form_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (input.getText().toString().equals("")) {
                        Toast.makeText(getActivity(), "Unchanged", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (mView.getTag().equals(Utils.TITLE_TAG)) {
                        if (!favoriteName.equals(input.getText().toString())) {
                            if (FileMgr.renameFavorite(favoriteName,
                                    input.getText().toString(),
                                    getActivity())) {
                                Toast.makeText(getActivity(), "Successfully updated name", Toast.LENGTH_LONG).show();
                                favoriteName = input.getText().toString();
                                tv_title.setText(favoriteName);
                            }
                        }

                    }
                    onResume();
                    dialog.dismiss();
                }
            });

            builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }

}