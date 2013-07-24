package com.krayzk9s.imgurholo;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Kurt Zimmer on 7/23/13.
 */
public class AlbumsFragment extends Fragment {

    private ArrayList<String> urls;
    private ArrayList<String> ids;
    ImageAdapter imageAdapter;
    AsyncTask<Void, Void, Void> async;

    public AlbumsFragment()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        urls = new ArrayList<String>();
        ids = new ArrayList<String>();
        View view = inflater.inflate(R.layout.image_layout, container, false);
        GridView gridview = (GridView) view;
        imageAdapter = new ImageAdapter(view.getContext());
        gridview.setAdapter(imageAdapter);
        gridview.setOnItemClickListener(new GridItemClickListener());
        async = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity activity = (MainActivity) getActivity();
                JSONObject imagesData = activity.makeGetCall("3/account/me/albums");
                try {
                    JSONArray imageArray = imagesData.getJSONArray("data");
                    for (int i = 0; i < imageArray.length(); i++) {
                        JSONObject imageData = imageArray.getJSONObject(i);
                        urls.add("http://imgur.com/" + imageData.getString("cover") + "m.png");
                        ids.add(imageData.getString("id"));
                    }
                } catch (Exception e) {
                    Log.e("Error!", e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                imageAdapter.notifyDataSetChanged();
            }
        };
        async.execute();
        return gridview;
    }
    public class ImageAdapter extends BaseAdapter {
        private Context mContext;

        public ImageAdapter(Context c) {
            mContext = c;
        }


        public int getCount() {
            return urls.size();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            final ImageView imageView = new SquareImageView(mContext);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            UrlImageViewHelper.setUrlDrawable(imageView, urls.get(position), R.drawable.icon);
            return imageView;
        }

    }
    private class GridItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public void selectItem(int position) {
        String id = ids.get(position);
        ImagesFragment fragment = new ImagesFragment();
        fragment.setImageCall("/3/album/" + id + "/images");
        MainActivity activity = (MainActivity) getActivity();
        activity.changeFragment(fragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        async.cancel(true);
    }
}