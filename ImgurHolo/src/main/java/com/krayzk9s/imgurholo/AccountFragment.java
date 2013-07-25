package com.krayzk9s.imgurholo;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class AccountFragment extends Fragment {

    String[] mMenuList;
    ArrayAdapter<String> adapter;
    private HashMap<String, String> accountData;

    public AccountFragment() {

    }

    @Override
    public void onCreate(Bundle save)
    {
        super.onCreate(save);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.account_layout, container, false);
        ListView mDrawerList = (ListView) view.findViewById(R.id.account_list);
        mMenuList = getResources().getStringArray(R.array.accountMenu);
        adapter = new ArrayAdapter<String>(view.getContext(),
                R.layout.drawer_list_item, mMenuList);
        mDrawerList.setAdapter(adapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        accountData = new HashMap<String, String>();
        AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                JSONObject accountInfoJSON = activity.makeGetCall("3/account/me");
                JSONObject statsJSON = activity.makeGetCall("3/account/me/stats");
                JSONObject likesJSON = activity.makeGetCall("3/account/me/likes");
                JSONObject settingsJSON = activity.makeGetCall("3/account/me/settings");

                try {
                    accountInfoJSON = accountInfoJSON.getJSONObject("data");
                    Log.d("URI", accountInfoJSON.toString());
                    Log.d("URI", Integer.toString(accountInfoJSON.getInt("id")));
                    accountData.put("id", Integer.toString(accountInfoJSON.getInt("id")));
                    accountData.put("reputation", Integer.toString(accountInfoJSON.getInt("reputation")));
                    Calendar accountCreationDate = Calendar.getInstance();
                    accountCreationDate.setTimeInMillis((long) accountInfoJSON.getInt("created") * 1000);
                    Log.d("URI", accountInfoJSON.getInt("created") + "");
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    String accountcreated = sdf.format(accountCreationDate.getTime());
                    Log.d("URI", accountcreated);
                    accountData.put("created", accountcreated);
                    accountData.put("bio", accountInfoJSON.getString("bio"));
                    accountData.put("name", accountInfoJSON.getString("url"));

                    statsJSON = statsJSON.getJSONObject("data");
                    accountData.put("total_images", Integer.toString(statsJSON.getInt("total_images")));
                    accountData.put("total_albums", Integer.toString(statsJSON.getInt("total_albums")));
                    accountData.put("disk_used", statsJSON.getString("disk_used"));
                    accountData.put("bandwidth_used", statsJSON.getString("bandwidth_used"));

                    JSONArray likesJSONArray = likesJSON.getJSONArray("data");
                    accountData.put("total_likes", String.valueOf(likesJSONArray.length()));

                    settingsJSON = settingsJSON.getJSONObject("data");
                    accountData.put("email", settingsJSON.getString("email"));
                    accountData.put("album_privacy", settingsJSON.getString("album_privacy"));
                    if(settingsJSON.getBoolean("public_images") == false)
                        accountData.put("public_images", "private");
                    else
                        accountData.put("public_images", "public");
                    if(settingsJSON.getBoolean("messaging_enabled") == false)
                        accountData.put("messaging_enabled", "disabled");
                    else
                        accountData.put("messaging_enabled", "enabled");
                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mMenuList[0] = mMenuList[0] + " (" + accountData.get("total_albums") + ")";
                mMenuList[1] = mMenuList[1] + " (" + accountData.get("total_images") + ")";
                mMenuList[2] = mMenuList[2] + " (" + accountData.get("total_likes") + ")";
                //mMenuList[3] = mMenuList[3] + " (" + accountData.get("total_comments") + ")";
                //mMenuList[4] = mMenuList[4] + " (" + accountData.get("total_messages") + ")";
                mMenuList[5] = mMenuList[5] + " " + accountData.get("created");
                if(accountData.get("bio") != "null")
                    mMenuList[6] = accountData.get("bio");
                else
                    mMenuList[6] = "No Biography";
                mMenuList[7] = "Your imgur e-mail is " + accountData.get("email");
                mMenuList[8] = "Your albums are " + accountData.get("album_privacy");
                mMenuList[9] = "Your images are " + accountData.get("public_images");
                mMenuList[10] = "Your messaging is " + accountData.get("messaging_enabled");
                mMenuList[11] = mMenuList[11] + " - " + accountData.get("disk_used");
                mMenuList[12] = mMenuList[12] + " - " + accountData.get("bandwidth_used");
                adapter.notifyDataSetChanged();
            }
        };
        async.execute();
        return view;
    }

    private void selectItem(int position) {
        final MainActivity activity = (MainActivity) getActivity();
        ImagesFragment imagesFragment;
        switch(position) {
            case 0:
                AlbumsFragment albumsFragment = new AlbumsFragment();
                activity.changeFragment(albumsFragment);
                break;
            case 1:
                imagesFragment = new ImagesFragment();
                imagesFragment.setImageCall("3/account/me/images/0");
                activity.changeFragment(imagesFragment);
                break;
            case 2:
                imagesFragment = new ImagesFragment();
                imagesFragment.setImageCall("3/account/me/likes");
                activity.changeFragment(imagesFragment);
                break;
            case 6:
                MessagingFragment messagingFragment = new MessagingFragment();
                activity.changeFragment(messagingFragment);
                break;
            case 7:
                final EditText input = new EditText(activity);
                new AlertDialog.Builder(activity).setTitle("Set e-mail")
                        .setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SettingsAsync settingsAsync = new SettingsAsync("email", input.getText(), accountData.get("name"));
                        settingsAsync.execute();
                        mMenuList[7] = "Your imgur email is " + input.getText();
                        adapter.notifyDataSetChanged();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                break;
            case 8:
                new AlertDialog.Builder(activity).setTitle("Set Album Privacy")
                        .setItems(R.array.privacy, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
                                SharedPreferences settings = activity.getSettings();
                                SharedPreferences.Editor editor = settings.edit();
                                int privacy = 0;
                                switch (whichButton) {
                                    case 0:
                                        privacy = 1;
                                        mMenuList[8] = "Your albums are private";
                                        break;
                                    case 1:
                                        privacy = 0;
                                        mMenuList[8] = "Your albums are public";
                                        break;
                                    default:
                                        break;
                                }
                                SettingsAsync settingsAsync = new SettingsAsync("album_privacy", privacy, accountData.get("name"));
                                settingsAsync.execute();
                                adapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                break;
            case 9:
                new AlertDialog.Builder(activity).setTitle("Set Image Privacy")
                        .setItems(R.array.privacy, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
                                SharedPreferences settings = activity.getSettings();
                                SharedPreferences.Editor editor = settings.edit();
                                int privacy = 0;
                                switch (whichButton) {
                                    case 0:
                                        privacy = 0;
                                        mMenuList[9] = "Your images are private";
                                        break;
                                    case 1:
                                        privacy = 1;
                                        mMenuList[9] = "Your images are public";
                                        break;
                                    default:
                                        break;
                                }
                                SettingsAsync settingsAsync = new SettingsAsync("public_images", privacy, accountData.get("name"));
                                settingsAsync.execute();
                                adapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                break;
            case 10:
                new AlertDialog.Builder(activity).setTitle("Enable/Disable Messaging")
                        .setItems(R.array.messaging, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainActivity activity = (MainActivity) getActivity();
                                SharedPreferences settings = activity.getSettings();
                                SharedPreferences.Editor editor = settings.edit();
                                int enable = 0;
                                switch (whichButton) {
                                    case 0:
                                        enable = 1;
                                        mMenuList[10] = "Your messaging is enabled";
                                        break;
                                    case 1:
                                        enable = 0;
                                        mMenuList[10] = "Your messaging is disabled";
                                        break;
                                    default:
                                        break;
                                }
                                SettingsAsync settingsAsync = new SettingsAsync("messaging_enabled", enable, accountData.get("name"));
                                settingsAsync.execute();
                                adapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                break;
            default:
                break;
        }
    }

    private class SettingsAsync extends AsyncTask<Void, Void, Void>
    {
        private Object data;
        private String settingName;
        private String username;
        public SettingsAsync(String _settingName, Object _data, String _username)
        {
            data = _data;
            settingName =_settingName;
            username = _username;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) getActivity();
            activity.makeSettingsPost(settingName, data, username);
            return null;
        }
    }


    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
}
