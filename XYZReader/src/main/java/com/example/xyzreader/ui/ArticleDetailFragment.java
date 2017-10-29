package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.utils.ParallaxPageTransformer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private NestedScrollView mScrollView;
    private ImageView mPhotoView;
    private FloatingActionButton mFabButton;
    private String mTitle;
    private Spanned mByline;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private boolean isDelayedLoading = false;
    private WebView mBodyView;
    private ProgressBar mArticleProgress;
    private int mPosition = 0;

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

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
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
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mFabButton = (FloatingActionButton) mRootView.findViewById(R.id.share_fab);
        mScrollView = (NestedScrollView) mRootView.findViewById(R.id.scrollview);
        mScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY > oldScrollY) {
                    mFabButton.hide();
                } else {
                    mFabButton.show();
                }

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    if (scrollY > 0) {
                        ((TextView)mRootView.findViewById(R.id.article_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.list_item_title_text_size));
                    } else {
                        ((TextView)mRootView.findViewById(R.id.article_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.detail_title_text_size));
                    }
                }
            }
        });

        mFabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        mBodyView = (WebView) mRootView.findViewById(R.id.article_body_web_view);
        mArticleProgress = (ProgressBar) mRootView.findViewById(R.id.article_progress);

        setArticleProgress();

        if (savedInstanceState != null) {
            mPosition = savedInstanceState.getInt("ARTICLE_SCROLL_POSITION");
        }

        bindViews();
        isDelayedLoading = true;
        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mScrollView.getScrollY() != 0) {
            outState.putInt("ARTICLE_SCROLL_POSITION", mScrollView.getScrollY()/ mScrollView.getMaxScrollAmount());
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            titleView.setText(mTitle);
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mByline = Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>");

            } else {
                // If date is before 1902, just show the string
                mByline = Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>");

            }
            bylineView.setText(mByline);

            if(isDelayedLoading) {
                loadBodyTextTask.execute();
            }

            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(final ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette p) {
                                        mMutedColor = p.getDarkMutedColor(0xFF333333);
                                        mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                        mRootView.findViewById(R.id.meta_bar)
                                                .setBackgroundColor(mMutedColor);
                                        mRootView.findViewById(R.id.app_bar)
                                                .setBackgroundColor(mMutedColor);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
        } else {
            mRootView.setVisibility(View.INVISIBLE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    AsyncTask<Void, Void, String> loadBodyTextTask = new AsyncTask<Void, Void, String>() {
        @Override
        protected String doInBackground(Void... params) {
            return mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br>");
        }

        @Override
        protected void onPostExecute(String spanned) {
            String htmlString = getStyledHtmlString(spanned);
            int px = (int) (getResources().getDimension(R.dimen.detail_body_text_size) / getResources().getDisplayMetrics().density);

            if(mBodyView == null) {
                return;
            }
            mBodyView.getSettings().setJavaScriptEnabled(true);
            mBodyView.getSettings().setDefaultFontSize(px);
            mBodyView.getSettings().setStandardFontFamily("sans-serif");
            mBodyView.loadData(htmlString, "text/html", "UTF-8");

            scrollArticleBodyToPosition();
        }
    };

    private void scrollArticleBodyToPosition() {
        mBodyView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final int scrollViewHeight = mBodyView.getHeight();
                        if (scrollViewHeight > 0) {
                            mScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            if (mPosition != 0) {
                                mScrollView.smoothScrollTo(0, (mPosition * mScrollView.getMaxScrollAmount()));
                            }
                        }
                    }
                });
            }
        });
    }

    private void setArticleProgress() {
        mBodyView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if(progress < 100 && mArticleProgress.getVisibility() == ProgressBar.INVISIBLE){
                    mArticleProgress.setVisibility(ProgressBar.VISIBLE);
                }

                mArticleProgress.setProgress(progress);
                if(progress == 100) {
                    mArticleProgress.setVisibility(ProgressBar.INVISIBLE);
                }
            }
        });
    }

    private String getStyledHtmlString(String str) {
        if (str == null || getActivity() == null) return "";
        String color = "#" + (Integer.toHexString(ContextCompat.getColor(getActivity(), R.color.textReading)).substring(2));
        return "<html><body><font color=\"" + color + "\">" + str + "</font></body></html>";
    }
}
