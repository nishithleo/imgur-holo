package com.krayzk9s.imgurholo.ui;

/*
 * Copyright 2013 Kurt Zimmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.krayzk9s.imgurholo.BuildConfig;
import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.SquareImageView;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;
import com.krayzk9s.imgurholo.tools.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by Kurt Zimmer on 7/23/13.
 */
public class GalleryFragment extends Fragment implements GetData {

    private ArrayList<String> urls;
    private ArrayList<JSONParcelable> ids;
    ImageAdapter imageAdapter;
    String sort;
    String gallery;
    int page;
    String subreddit;
    String memeType;
    String window;
    ArrayAdapter<CharSequence> mSpinnerAdapter;
    ActionBar actionBar;
    int selectedIndex;
    SearchView mSearchView;
    MenuItem searchItem;
    String search;
    CharSequence spinner;
    int lastInView = -1;
    TextView errorText;
    GridView gridview;
    int oldwidth = 0;
    private boolean fetchingImages;
    TextView noImageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        final MainActivity activity = (MainActivity) getActivity();
        final SharedPreferences settings = activity.getSettings();
        if (savedInstanceState != null) {
            gallery = savedInstanceState.getString("gallery");
            sort = savedInstanceState.getString("sort");
            window = savedInstanceState.getString("window");
            subreddit = savedInstanceState.getString("subreddit");
            search = savedInstanceState.getString("search");
            urls = savedInstanceState.getStringArrayList("urls");
            ids = savedInstanceState.getParcelableArrayList("ids");
            page = savedInstanceState.getInt("page");
            selectedIndex = savedInstanceState.getInt("selectedIndex");
            spinner = savedInstanceState.getCharSequence("spinner");
        } else {
            page = 0;
            subreddit = "pics";
            memeType = "top";
            gallery = settings.getString("DefaultGallery", "hot");
            ArrayList<String> galleryOptions = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.galleryOptions)));
            sort = "viral";
            window = "day";
            urls = new ArrayList<String>();
            ids = new ArrayList<JSONParcelable>();
            selectedIndex = galleryOptions.indexOf(gallery);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        activity.setTitle("Gallery");
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if(activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        menu.findItem(R.id.action_sort).setVisible(true);
        menu.findItem(R.id.subreddit).setVisible(true);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);

        menu.findItem(R.id.action_search).setVisible(true);

        searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();
       SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                // Do nothing
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d("searching", mSearchView.getQuery() + "");
                mSpinnerAdapter.add("search: " + mSearchView.getQuery());
                subreddit = null;
                if (mSpinnerAdapter.getCount() > 6) {
                    mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                }
                gallery = "search";
                search = mSearchView.getQuery() + "";
                searchItem.collapseActionView();
                actionBar.setSelectedNavigationItem(5);
                makeGallery();
                return true;
            }
        };
        mSearchView.setOnQueryTextListener(queryTextListener);
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
        menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MainActivity activity = (MainActivity) getActivity();
        page = 0;
        switch (item.getItemId()) {
            case R.id.action_sort:
                Log.d("sorting", "sorting");
                return true;
            case R.id.action_refresh:
                page = 0;
                makeGallery();
                return true;
            case R.id.subreddit:
                gallery = "subreddit";
                sort = "time";
                final EditText subredditText = new EditText(activity);
                subredditText.setSingleLine();
                new AlertDialog.Builder(activity).setTitle("Choose SubReddit").setView(subredditText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(subredditText.getText() != null)
                            subreddit = subredditText.getText().toString();
                        mSpinnerAdapter.add("/r/" + subreddit);
                        search = null;
                        if (mSpinnerAdapter.getCount() > 6) {
                            mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                        }
                        mSpinnerAdapter.notifyDataSetChanged();
                        actionBar.setSelectedNavigationItem(5);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;
            case R.id.menuSortPopularity:
                sort = "viral";
                break;
            case R.id.menuSortNewest:
                sort = "time";
                break;
            case R.id.menuSortTop:
                sort = "top";
                break;
            case R.id.menuSortDay:
                sort = "top";
                window = "day";
                break;
            case R.id.menuSortWeek:
                sort = "top";
                window = "week";
                break;
            case R.id.menuSortMonth:
                sort = "top";
                window = "month";
                break;
            case R.id.menuSortYear:
                sort = "top";
                window = "year";
                break;
            case R.id.menuSortAll:
                sort = "top";
                window = "all";
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        makeGallery();
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        actionBar = activity.getActionBar();
        SharedPreferences settings = activity.getSettings();
        View view;
        view = inflater.inflate(R.layout.image_layout, container, false);
        errorText = (TextView) view.findViewById(R.id.error);
        gridview = (GridView) view.findViewById(R.id.grid_layout);
        gridview.setColumnWidth(Utils.dpToPx(Integer.parseInt(settings.getString("IconSize", "120")), getActivity()));
        imageAdapter = new ImageAdapter(view.getContext());
        noImageView = (TextView) view.findViewById(R.id.no_images);
        gridview.setAdapter(imageAdapter);
            gridview.setOnItemClickListener(new GridItemClickListener());
            gridview.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if(imageAdapter.getNumColumns() == 0 || gridview.getWidth() != oldwidth)
                                setNumColumns();
                        }
                    });
            gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }
                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if(lastInView == -1)
                        lastInView = firstVisibleItem;
                    else if (lastInView > firstVisibleItem) {
                        actionBar.show();
                        lastInView = firstVisibleItem;
                    }
                    else if (lastInView < firstVisibleItem) {
                        actionBar.hide();
                        lastInView = firstVisibleItem;
                    }
                    int lastInScreen = firstVisibleItem + visibleItemCount;
                    if ((lastInScreen == totalItemCount) && urls != null && urls.size() > 0 && !fetchingImages) {
                        if(!gallery.equals("search")) {
                            page += 1;
                            getImages();
                        }
                    }
                }
            });
        setupActionBar();
        if(savedInstanceState == null && urls.size() < 1) {
            Log.d("urls", urls.size() + "");
            makeGallery();
        }
        return view;
    }

    private void setNumColumns() {
        Log.d("Setting Columns", "Setting Columns");
        MainActivity activity = (MainActivity) getActivity();
        if(activity != null) {
            oldwidth = gridview.getWidth();
            SharedPreferences settings = activity.getSettings();
            Log.d("numColumnsWidth", gridview.getWidth()+"");
            Log.d("numColumnsIconWidth", Utils.dpToPx((Integer.parseInt(settings.getString("IconSize", "120"))), getActivity())+"");
            final int numColumns = (int) Math.floor(
                    gridview.getWidth() / (Utils.dpToPx((Integer.parseInt(settings.getString("IconSize", "120"))), getActivity()) + Utils.dpToPx(4, getActivity())));
            if (numColumns > 0) {
                imageAdapter.setNumColumns(numColumns);
                if (BuildConfig.DEBUG) {
                    Log.d("NUMCOLS", "onCreateView - numColumns set to " + numColumns);
                }
                imageAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (sort == null || (sort.equals("viral") && !gallery.equals("top")))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setChecked(true);
        else if (sort.equals("time") && !gallery.equals("top"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setChecked(true);
        else if (window.equals("day"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setChecked(true);
        else if (window.equals("week"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setChecked(true);
        else if (window.equals("month"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setChecked(true);
        else if (window.equals("year"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setChecked(true);
        else if (window.equals("all"))
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setChecked(true);

        if (gallery == null || gallery.equals("hot") || gallery.equals("user")) {
            menu.findItem(R.id.action_sort).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
            //menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
        } else if (gallery.equals("top")) {
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortNewest).setVisible(false);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
        } else if (gallery.equals("memes")) {
            //menu.findItem(R.id.action_sort).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(true);
            //menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(true);
            if (sort.equals("top")) {
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
            }
        } else if (gallery.equals("random")) {
            menu.findItem(R.id.action_sort).setVisible(false);
        } else if (gallery.equals("subreddit")) {
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(true);
            menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortPopularity).setVisible(false);
            //menu.findItem(R.id.action_sort).setVisible(true);
            if (sort.equals("top")) {
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortTop).setVisible(false);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortDay).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortWeek).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortMonth).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortYear).setVisible(true);
                menu.findItem(R.id.action_sort).getSubMenu().findItem(R.id.menuSortAll).setVisible(true);
            }
        }
    }

    private void makeGallery() {
        urls = new ArrayList<String>();
        ids = new ArrayList<JSONParcelable>();
        imageAdapter.notifyDataSetChanged();
        MainActivity activity = (MainActivity) getActivity();
        activity.invalidateOptionsMenu();
        getImages();
    }


    private void getImages() {
        fetchingImages = true;
        errorText.setVisibility(View.GONE);
        String call = "";
        if (gallery.equals("hot") || gallery.equals("top") || gallery.equals("user")) {
            call = "3/gallery/" + gallery + "/" + sort + "/" + window + "/" + page;
        } else if (gallery.equals("memes")) {
            call = "3/gallery/g/memes/" + sort + "/" + window + "/" + page;
        } else if (gallery.equals("random")) {
            call = "3/gallery/random/random/" + page;
        } else if (gallery.equals("subreddit")) {
            call = "3/gallery/r/" + subreddit + "/" + sort + "/" + window + "/" + page;
        } else if (gallery.equals("search")) {
            call = "3/gallery/search?q=" + search;
        }
        Fetcher fetcher = new Fetcher(this, call, ApiCall.GET, null, ((ImgurHoloActivity)getActivity()).getApiCall(), "images");
        fetcher.execute();
    }

    public void handleException(Exception e, String tag) {
        Log.e("Error!", e.toString());
        noImageView.setVisibility(View.VISIBLE);
    }

    public void onGetObject(Object object, String tag) {
        JSONObject data = (JSONObject) object;
        MainActivity activity = (MainActivity) getActivity();
        Log.d("imagesData", "checking");
        if(activity == null || data == null) {
            return;
        }
        Log.d("imagesData", "failed");
        try {
            Log.d("URI", data.toString());
            JSONArray imageArray = data.getJSONArray("data");
            for (int i = 0; i < imageArray.length(); i++) {
                JSONObject imageData = imageArray.getJSONObject(i);
                Log.d("Data", imageData.toString());
                SharedPreferences settings = activity.getSettings();
                String s = settings.getString("IconQuality", "m");
                try {
                    if (imageData.has("is_album") && imageData.getBoolean("is_album")) {
                        if (!urls.contains("http://imgur.com/" + imageData.getString("cover") + s + ".png")) {
                            urls.add("http://imgur.com/" + imageData.getString("cover") + s + ".png");
                            UrlImageViewHelper.loadUrlDrawable(activity, "http://imgur.com/" + imageData.getString("cover") + s + ".png");
                            JSONParcelable dataParcel = new JSONParcelable();
                            dataParcel.setJSONObject(imageData);
                            ids.add(dataParcel);
                        }
                    }
                    else {
                        if (!urls.contains("http://imgur.com/" + imageData.getString("id") + s + ".png"))
                        {
                            urls.add("http://imgur.com/" + imageData.getString("id") + s + ".png");
                            UrlImageViewHelper.loadUrlDrawable(activity, "http://imgur.com/" + imageData.getString("id") + s + ".png");
                            JSONParcelable dataParcel = new JSONParcelable();
                            dataParcel.setJSONObject(imageData);
                            ids.add(dataParcel);
                        }
                    }
                }
                catch (RejectedExecutionException e) {
                    Log.e("Rejected", e.toString());
                }
            }
        } catch (JSONException e) {
            Log.e("Error!", e.toString());
        }
        imageAdapter.notifyDataSetChanged();
        fetchingImages = false;
    }

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private int mNumColumns;

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }

        public ImageAdapter(Context c) {
            mContext = c;
        }

        @Override
        public long getItemId(int position) {
            return position < mNumColumns ? 0 : position - mNumColumns;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < mNumColumns) ? 1 : 0;
        }

        public int getCount() {
            return urls.size()  + mNumColumns;
        }

        public Object getItem(int position) {
            return position < mNumColumns ?
                    null :urls.get(position - mNumColumns);
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < mNumColumns) {
                if (convertView == null) {
                    convertView = new View(mContext);
                }
                if(getActivity().getActionBar() != null)
                convertView.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, getActivity().getActionBar().getHeight()));
                return convertView;
            }
            else {
                ImageView imageView;
                if(convertView == null) {
                    imageView = new SquareImageView(mContext);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
                else {
                    imageView = (ImageView) convertView;
                }
                UrlImageViewHelper.setUrlDrawable(imageView, urls.get(position - mNumColumns));
                return imageView;
            }
        }

    }

    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position - imageAdapter.getNumColumns());
        }
    }

    public void selectItem(int position) {
        Intent intent = new Intent();
        intent.putExtra("start", position);
        intent.putExtra("ids", new ArrayList<JSONParcelable>(ids));
        intent.setAction(ImgurHoloActivity.IMAGE_PAGER_INTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity activity = (MainActivity) getActivity();
        ActionBar actionBar = activity.getActionBar();
        if(actionBar != null)
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current state
        savedInstanceState.putString("gallery", gallery);
        savedInstanceState.putString("sort", sort);
        savedInstanceState.putString("window", window);
        savedInstanceState.putString("subreddit", subreddit);
        savedInstanceState.putString("search", search);
        savedInstanceState.putStringArrayList("urls", urls);
        savedInstanceState.putParcelableArrayList("ids", ids);
        savedInstanceState.putInt("page", page);
        savedInstanceState.putInt("selectedIndex", selectedIndex);
        if(mSpinnerAdapter != null && mSpinnerAdapter.getCount() > 5)
            savedInstanceState.putCharSequence("spinner", mSpinnerAdapter.getItem(5));
        else
            savedInstanceState.putCharSequence("spinner", null);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private void setupActionBar() {
        MainActivity activity = (MainActivity) getActivity();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        Resources res = activity.getResources();
        List<CharSequence> options = new ArrayList(Arrays.asList(res.getStringArray(R.array.galleryOptions)));
        mSpinnerAdapter = new ArrayAdapter<CharSequence>(activity, android.R.layout.simple_spinner_dropdown_item
                , options);
        if(spinner != null)
            mSpinnerAdapter.add(spinner);
        ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int i, long l) {
                String newGallery = "";
                    switch (i) {
                        case 0:
                            newGallery = "hot";
                            sort = "viral";
                            break;
                        case 1:
                            newGallery = "top";
                            sort = "day";
                            break;
                        case 2:
                            newGallery = "user";
                            sort = "viral";
                            break;
                        case 3:
                            newGallery = "memes";
                            sort = "viral";
                            break;
                        case 4:
                            newGallery = "random";
                            break;
                    }
                    if(newGallery.equals(gallery))
                        return true;
                    else if(i < 5)
                        gallery = newGallery;
                    selectedIndex = i;
                    if (mSpinnerAdapter.getCount() > 5 && !gallery.equals("subreddit") && !gallery.equals("search")) {
                        mSpinnerAdapter.remove(mSpinnerAdapter.getItem(5));
                        subreddit = null;
                        search = null;
                    }
                    page = 0;
                    makeGallery();
                return true;
            }
        };
        Log.d("gallery", gallery);
        if(selectedIndex == 5) {
            if(subreddit != null)
                mSpinnerAdapter.add("/r/" + subreddit);
            else
                mSpinnerAdapter.add("search: " + search);
        }
        actionBar.setSelectedNavigationItem(selectedIndex);
        Log.d("Setting Item", "Setting Item");
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
    }
}