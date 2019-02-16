package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private long mItemId;
    private String mTitle;
    private String mAuthor;
    private String mPublishedDate;
    private String mBody;
    private String mPhotoURL;

    private View mRootView;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private AppBarLayout mAppBarLayout;
    private NestedScrollView mScrollView;
    private ImageView mPhotoView;
    private TextView mBylineView;
    private TextView mBodyView;

    private boolean mIsAppBarExpanded = true;

    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mItemId = arguments.getLong(ARG_ITEM_ID);
        }

//        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
//        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
//                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article, container, false);
        mAppBarLayout = mRootView.findViewById(R.id.appbar);
        mCollapsingToolbar = mRootView.findViewById(R.id.collapsing_toolbar);
        mScrollView = mRootView.findViewById(R.id.scrollview);

        final Toolbar toolbar = mRootView.findViewById(R.id.anim_toolbar);
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        appCompatActivity.setSupportActionBar(toolbar);
        if (appCompatActivity.getSupportActionBar() != null) {
            appCompatActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mPhotoView = mRootView.findViewById(R.id.photo);
        mBylineView = mRootView.findViewById(R.id.article_byline);
        mBylineView.setMovementMethod(new LinkMovementMethod());
        mBodyView = mRootView.findViewById(R.id.article_body);

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //  Vertical offset == 0 indicates appBar is fully expanded.
                if (Math.abs(verticalOffset) > 200) {
                    mIsAppBarExpanded = false;
                    getActivity().invalidateOptionsMenu();
                } else {
                    mIsAppBarExpanded = true;
                    getActivity().invalidateOptionsMenu();
                }
            }
        });

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        return mRootView;
    }

    private void populateUI() {
        mCollapsingToolbar.setTitle(mTitle != null ? mTitle : "");

        Date publishedDate = parsePublishedDate(mPublishedDate);
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            mBylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mAuthor
                            + "</font>"));
        } else {
            // If date is before 1902, just show the string
            mBylineView.setText(Html.fromHtml(outputFormat.format(publishedDate)
                    + " by <font color='#ffffff'>"
                    + mAuthor
                    + "</font>"));
        }

        mBodyView.setText(mBody != null
                ? Html.fromHtml(mBody.replaceAll("(\r\n|\n)", "<br />"))
                : "");

        if (mPhotoURL != null) {
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mPhotoURL, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            // TODO
                        }
                    });
        }
    }

    private static Date parsePublishedDate(String date) {
        if (date != null) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }
        return new Date();
    }

    @NonNull
    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
        if (!isAdded() && cursor != null) {
            cursor.close();
            return;
        }

        if (cursor != null && !cursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            cursor.close();
        }

        if (cursor != null) {
            mTitle = cursor.getString(ArticleLoader.Query.TITLE);
            mAuthor = cursor.getString(ArticleLoader.Query.AUTHOR);
            mPublishedDate = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            mBody = cursor.getString(ArticleLoader.Query.BODY);
            mPhotoURL = cursor.getString(ArticleLoader.Query.PHOTO_URL);
        }

        populateUI();
    }

    @Override
    public void onLoaderReset(@NonNull android.support.v4.content.Loader<Cursor> loader) {
        // Do nothing
    }
}
