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

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.krayzk9s.imgurholo.BuildConfig;
import com.krayzk9s.imgurholo.R;
import com.krayzk9s.imgurholo.activities.ImageSelectActivity;
import com.krayzk9s.imgurholo.activities.ImgurHoloActivity;
import com.krayzk9s.imgurholo.activities.MainActivity;
import com.krayzk9s.imgurholo.libs.JSONParcelable;
import com.krayzk9s.imgurholo.libs.SquareImageView;
import com.krayzk9s.imgurholo.services.DownloadService;
import com.krayzk9s.imgurholo.tools.AddImagesToAlbumAsync;
import com.krayzk9s.imgurholo.tools.ApiCall;
import com.krayzk9s.imgurholo.tools.CommentsAsync;
import com.krayzk9s.imgurholo.tools.Fetcher;
import com.krayzk9s.imgurholo.tools.GetData;
import com.krayzk9s.imgurholo.tools.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 7/22/13.
 */
public class ImagesFragment extends Fragment implements GetData {

    public boolean selecting = false;
    ImageAdapter imageAdapter;
    String imageCall;
    GridView gridview;
    MultiChoiceModeListener multiChoiceModeListener;
    ArrayList<String> intentReturn;
    String albumId;
    JSONObject galleryAlbumData;
    TextView noImageView;
    int page;
    boolean gettingImages = false;
    int lastInView = -1;
    private ArrayList<String> urls;
    private ArrayList<JSONParcelable> ids;
    TextView errorText;
    final static String DELETE = "delete";
    final static String IMAGES = "images";

    public ImagesFragment() {
        page = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setTitle("Images");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if(bundle.containsKey("id"))
            albumId = bundle.getString("id");
        else
            albumId = null;
        imageCall = bundle.getString("imageCall");
        if(bundle.containsKey("albumData")) {
            JSONParcelable dataParcel = bundle.getParcelable("albumData");
            if(dataParcel != null)
                galleryAlbumData = dataParcel.getJSONObject();
        }
        else
            galleryAlbumData = null;

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        if (activity.getApiCall().settings.getString("theme", MainActivity.HOLO_LIGHT).equals(MainActivity.HOLO_LIGHT))
            inflater.inflate(R.menu.main, menu);
        else
            inflater.inflate(R.menu.main_dark, menu);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(true);
        menu.findItem(R.id.action_download).setVisible(true);
        if (albumId != null && galleryAlbumData == null) {
            menu.findItem(R.id.action_new).setVisible(true);
        }
        if (albumId != null && galleryAlbumData != null) {
            menu.findItem(R.id.action_comments).setVisible(true);
        }
        if (albumId != null) {
            menu.findItem(R.id.action_copy).setVisible(true);
            menu.findItem(R.id.action_share).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        final Activity activity = getActivity();
        Toast toast;
        int duration;
        switch (item.getItemId()) {
            case R.id.action_download:
                duration = Toast.LENGTH_SHORT;
                toast = Toast.makeText(activity, "Downloading " + urls.size() + " images! This may take a while...", duration);
                toast.show();
                Intent serviceIntent = new Intent(activity, DownloadService.class);
                serviceIntent.putParcelableArrayListExtra("ids", ids);
                activity.startService(serviceIntent);
                return true;
            case R.id.action_refresh:
                urls = new ArrayList<String>();
                ids = new ArrayList<JSONParcelable>();
                page = 0;
                imageAdapter.notifyDataSetChanged();
                getImages();
                return true;
            case R.id.action_copy:
                    ClipboardManager clipboard = (ClipboardManager)
                            activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("imgur Link", "http://imgur.com/a/" + albumId);
                    clipboard.setPrimaryClip(clip);
                    duration = Toast.LENGTH_SHORT;
                    toast = Toast.makeText(activity, "Copied!", duration);
                    toast.show();
                return true;
            case R.id.action_share:
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.putExtra(Intent.EXTRA_TEXT, "http://imgur.com/a/" + albumId);
                startActivity(intent);
                return true;
            case R.id.action_new:
                Intent i = new Intent(this.getActivity().getApplicationContext(), ImageSelectActivity.class);
                startActivityForResult(i, 1);
                //select image
                return true;
            case R.id.action_comments:
                CommentsAsync commentsAsync = new CommentsAsync(((ImgurHoloActivity)getActivity()).getApiCall(), galleryAlbumData);
                commentsAsync.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("requestcode", requestCode + "");
        if(data.getExtras() == null)
            return;
        Object bundle = data.getExtras().get("data");
        Log.d("HELO", bundle.getClass().toString());
        switch (requestCode) {
            case 1:
                super.onActivityResult(requestCode, resultCode, data);
                ArrayList<String> imageIds = data.getStringArrayListExtra("data");
                if(imageIds != null)
                    Log.d("Ids!", imageIds.toString());
                AddImagesToAlbumAsync imageAsync = new AddImagesToAlbumAsync(imageIds, true, ((ImgurHoloActivity)getActivity()).getApiCall(), albumId);
                imageAsync.execute();
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (urls == null) {
            urls = new ArrayList<String>();
            ids = new ArrayList<JSONParcelable>();
        }
        View view = inflater.inflate(R.layout.image_layout, container, false);
        gridview = (GridView) view.findViewById(R.id.grid_layout);
        errorText = (TextView) view.findViewById(R.id.error);
        noImageView = (TextView) view.findViewById(R.id.no_images);
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        ImgurHoloActivity activity = (ImgurHoloActivity) getActivity();
        final SharedPreferences settings = activity.getApiCall().settings;
        gridview.setColumnWidth(Utils.dpToPx(Integer.parseInt(settings.getString("IconSize", "120")), getActivity()));
        gridview.setOnItemClickListener(new GridItemClickListener());
        gridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        multiChoiceModeListener = new MultiChoiceModeListener();
        gridview.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (imageAdapter.getNumColumns() == 0) {
                            Log.d("numColumnsWidth", gridview.getWidth() + "");
                            Log.d("numColumnsIconWidth", Utils.dpToPx((Integer.parseInt(settings.getString("IconSize", "120"))), getActivity()) + "");
                            final int numColumns = (int) Math.floor(
                                    gridview.getWidth() / (Utils.dpToPx((Integer.parseInt(settings.getString("IconSize", "120"))), getActivity()) + Utils.dpToPx(2, getActivity())));
                            if (numColumns > 0) {
                                imageAdapter.setNumColumns(numColumns);
                                if (BuildConfig.DEBUG) {
                                    Log.d("NUMCOLS", "onCreateView - numColumns set to " + numColumns);
                                }
                                imageAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
        gridview.setMultiChoiceModeListener(multiChoiceModeListener);
        if (albumId == null) {
            gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (lastInView == -1)
                        lastInView = firstVisibleItem;
                    else if (getActivity().getActionBar() != null && lastInView > firstVisibleItem) {
                        getActivity().getActionBar().show();
                        lastInView = firstVisibleItem;
                    } else if (getActivity().getActionBar() != null && lastInView < firstVisibleItem) {
                        getActivity().getActionBar().hide();
                        lastInView = firstVisibleItem;
                    }
                    int lastInScreen = firstVisibleItem + visibleItemCount;
                    if ((lastInScreen == totalItemCount) && urls != null && urls.size() > 0 && !gettingImages) {
                        gettingImages = true;
                        page += 1;
                        getImages();
                    }
                }
            });
        }
        if (savedInstanceState == null && urls.size() == 0) {
            getImages();

        } else if (savedInstanceState != null) {
            urls = savedInstanceState.getStringArrayList("urls");
            ids = savedInstanceState.getParcelableArrayList("ids");
        }
        return view;
    }

    public void onGetObject(Object object, String tag) {
        if(tag.equals(DELETE)) {
            urls = new ArrayList<String>();
            ids = new ArrayList<JSONParcelable>();
            page = 0;
            imageAdapter.notifyDataSetChanged();
            getImages();
        }
        else if(tag.equals(IMAGES)) {
            JSONObject data = (JSONObject) object;
            if(data == null)
                return;
            Boolean changed = false;
            JSONArray imageArray;
            try {
                if (data.optJSONObject("data") != null)
                    imageArray = data.getJSONObject("data").getJSONArray("images");
                else
                    imageArray = data.getJSONArray("data");
                Log.d("single image array", imageArray.toString());
                for (int i = 0; i < imageArray.length(); i++) {
                    JSONObject imageData = imageArray.getJSONObject(i);
                    Log.d("Data", imageData.toString());
                    if(imageCall.equals("3/account/me/likes") && !imageData.getBoolean("favorite"))
                        continue;
                    JSONParcelable dataParcel = new JSONParcelable();
                    dataParcel.setJSONObject(imageData);
                    if (imageData.has("is_album") && imageData.getBoolean("is_album") && !urls.contains("http://imgur.com/" + imageData.getString("cover") + "m.png")) {
                        urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                        ids.add(dataParcel);
                        changed = true;
                    } else if(!urls.contains("http://imgur.com/" + imageData.getString("id") + "m.png")) {
                        urls.add("http://imgur.com/" + imageData.getString("id") + "m.png");
                        ids.add(dataParcel);
                        changed = true;
                    }
                }
            }
            catch (JSONException e) {
                Log.e("JSON error!", "oops");
            }
            gettingImages = !changed;
            if (urls.size() > 0)
                imageAdapter.notifyDataSetChanged();
            else if (urls.size() == 0 && noImageView != null)
                noImageView.setVisibility(View.VISIBLE);
            else
                gettingImages = true;
        }
    }

    public void handleException(Exception e, String tag) {
        Log.e("Error!", e.toString());
        noImageView.setVisibility(View.VISIBLE);
    }

    private void getImages() {
        errorText.setVisibility(View.GONE);
        Fetcher fetcher = new Fetcher(this, imageCall + "/" + page, ApiCall.GET, null, ((ImgurHoloActivity)getActivity()).getApiCall(), IMAGES);
        fetcher.execute();
    }

    public void selectItem(int position) {
        if (!selecting) {
            ArrayList<JSONParcelable> idCopy = ids;
            Intent intent = new Intent();
            intent.putExtra("start", position);
            intent.putExtra("ids", idCopy);
            intent.setAction(ImgurHoloActivity.IMAGE_PAGER_INTENT);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivity(intent);
        }
    }

    public ImagesFragment getOuter() {
        return this;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList("urls", urls);
        savedInstanceState.putParcelableArrayList("ids", ids);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public class ImageAdapter extends BaseAdapter {
        CheckableLayout l;
        SquareImageView i;
        private Context mContext;
        private int mNumColumns;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getNumColumns() {
            return mNumColumns;
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
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

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public int getCount() {
            if(urls != null)
                return urls.size() + mNumColumns;
            else
                return 0;
        }

        public Object getItem(int position) {
            return position < mNumColumns ?
                    null : ids.get(position - mNumColumns);
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
            } else {
                if (convertView == null) {
                    i = new SquareImageView(mContext);
                    i.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    l = new CheckableLayout(getActivity());
                    l.setPadding(2, 2, 2, 2);
                    l.addView(i);
                } else {
                    l = (CheckableLayout) convertView;
                    i = (SquareImageView) l.getChildAt(0);
                }
                UrlImageViewHelper.setUrlDrawable(i, urls.get(position - mNumColumns));
                return l;
            }
        }
    }

    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position - imageAdapter.getNumColumns());
        }
    }

    public class MultiChoiceModeListener implements GridView.MultiChoiceModeListener {
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("Select Items");
            mode.setSubtitle("One item selected");
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.images_multi, menu);
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    if (albumId == null) {
                        getChecked();
                        for(int i = 0; i < intentReturn.size(); i++) {
                            Fetcher fetcher = new Fetcher(getOuter(), "3/image/" + intentReturn.get(i), ApiCall.DELETE, null, ((ImgurHoloActivity)getActivity()).getApiCall(), DELETE);
                            fetcher.execute();
                        }
                    }
                    break;
                default:
                    break;
            }
            mode.finish();
            return false;
        }

        public void onDestroyActionMode(ActionMode mode) {
            if (selecting) {
                Intent intent = new Intent();
                getChecked();
                intent.putExtra("data", intentReturn);
                ImageSelectActivity imageSelectActivity = (ImageSelectActivity) getActivity();
                imageSelectActivity.setResult(ImageSelectActivity.RESULT_OK, intent);
                imageSelectActivity.finish();
            }
        }

        private void getChecked() {
            intentReturn = new ArrayList<String>();
            try {
                for (int i = 0; i < gridview.getCount(); i++) {
                    if (gridview.isItemChecked(i)) {
                        JSONParcelable imageData = (JSONParcelable) imageAdapter.getItem(i);
                        intentReturn.add(imageData.getJSONObject().getString("id"));
                        Log.d("checkedid", imageData.getJSONObject().getString("id"));
                    }
                }
            } catch (JSONException e) {
                Log.e("Error!", e.toString());
            }
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
            int selectCount = gridview.getCheckedItemCount();
            Log.d("count", "" + selectCount);
            switch (selectCount) {
                case 1:
                    mode.setSubtitle("One item selected");
                    break;
                default:
                    mode.setSubtitle("" + selectCount + " items selected");
                    break;
            }
        }
    }

    public class CheckableLayout extends FrameLayout implements Checkable {
        private boolean mChecked;

        public CheckableLayout(Context context) {
            super(context);
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
            setBackgroundDrawable(checked ?
                    getResources().getDrawable(R.drawable.select_background)
                    : null);
        }

        public void toggle() {
            setChecked(!mChecked);
        }

    }
}
