package ml.docilealligator.infinityforreddit;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewSubredditDetailActivity extends AppCompatActivity {

    static final String EXTRA_SUBREDDIT_NAME = "ESN";
    static final String EXTRA_SUBREDDIT_ID = "ESI";

    private SubredditViewModel mSubredditViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_subreddit_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String id = getIntent().getExtras().getString(EXTRA_SUBREDDIT_ID);
        String subredditName = getIntent().getExtras().getString(EXTRA_SUBREDDIT_NAME);

        final String title = "r/" + subredditName;
        setTitle(title);

        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_layout_view_subreddit_detail_activity);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout_view_subreddit_detail_activity);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbarLayout.setTitle(title);
                    isShow = true;
                } else if(isShow) {
                    collapsingToolbarLayout.setTitle(" ");//carefull there should a space between double quote otherwise it wont work
                    isShow = false;
                }
            }
        });

        final ImageView bannerImageView = findViewById(R.id.banner_image_view_view_subreddit_detail_activity);
        final CircleImageView iconCircleImageView = findViewById(R.id.icon_circle_image_view_view_subreddit_detail_activity);
        final TextView subredditNameTextView = findViewById(R.id.subreddit_name_text_view_view_subreddit_detail_activity);
        final TextView nSubscribersTextView = findViewById(R.id.subscriber_count_text_view_view_subreddit_detail_activity);
        final TextView nOnlineSubscribersTextView = findViewById(R.id.online_subscriber_count_text_view_view_subreddit_detail_activity);
        final TextView descriptionTextView = findViewById(R.id.description_text_view_view_subreddit_detail_activity);
        final RequestManager glide = Glide.with(ViewSubredditDetailActivity.this);

        SubredditViewModel.Factory factory = new SubredditViewModel.Factory(getApplication(), id);
        mSubredditViewModel = ViewModelProviders.of(this, factory).get(SubredditViewModel.class);
        mSubredditViewModel.getSubredditLiveData().observe(this, new Observer<SubredditData>() {
            @Override
            public void onChanged(@Nullable SubredditData subredditData) {
                if(subredditData != null) {
                    if(!subredditData.getBannerUrl().equals("") && !subredditData.getBannerUrl().equals("null")) {
                        glide.load(subredditData.getBannerUrl()).into(bannerImageView);
                    }

                    if(subredditData.getIconUrl().equals("") || subredditData.getIconUrl().equals("null")) {
                        glide.load(getDrawable(R.drawable.subreddit_default_icon)).into(iconCircleImageView);
                    } else {
                        glide.load(subredditData.getIconUrl()).into(iconCircleImageView);
                    }

                    subredditNameTextView.setText(subredditData.getName());
                    String nSubscribers = getString(R.string.subscribers_number_detail, subredditData.getNSubscribers());
                    nSubscribersTextView.setText(nSubscribers);
                    descriptionTextView.setText(subredditData.getDescription());
                }
            }
        });

        new FetchSubredditData(Volley.newRequestQueue(this), subredditName).querySubredditData(new FetchSubredditData.FetchSubredditDataListener() {
            @Override
            public void onFetchSubredditDataSuccess(String response) {
                new ParseSubredditData().parseComment(response, new ParseSubredditData.ParseSubredditDataListener() {
                    @Override
                    public void onParseSubredditDataSuccess(SubredditData subredditData, int nCurrentOnlineSubscribers) {
                        new InsertSubredditDataAsyncTask(SubredditRoomDatabase.getDatabase(ViewSubredditDetailActivity.this), subredditData)
                                .execute();
                        String nOnlineSubscribers = getString(R.string.online_subscribers_number_detail, nCurrentOnlineSubscribers);
                        nOnlineSubscribersTextView.setText(nOnlineSubscribers);
                    }

                    @Override
                    public void onParseSubredditDataFail() {
                        Toast.makeText(ViewSubredditDetailActivity.this, "Cannot fetch subreddit info", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFetchSubredditDataFail() {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    private static class InsertSubredditDataAsyncTask extends AsyncTask<Void, Void, Void> {

        private final SubredditDao mSubredditDao;
        private SubredditData subredditData;

        InsertSubredditDataAsyncTask(SubredditRoomDatabase subredditDb, SubredditData subredditData) {
            mSubredditDao = subredditDb.subredditDao();
            this.subredditData = subredditData;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mSubredditDao.insert(subredditData);
            return null;
        }
    }
}
