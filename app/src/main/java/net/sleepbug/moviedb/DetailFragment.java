package net.sleepbug.moviedb;

/**
 * Created by panzertax on 13/09/15.
 */
import android.support.v4.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.content.Intent;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import com.activeandroid.query.Select;
import android.view.LayoutInflater;
import net.sleepbug.moviedb.data.*;
import net.sleepbug.moviedb.responses.*;
import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.Callback;
import retrofit.client.Response;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import java.util.List;

public class DetailFragment extends Fragment {
    private static final String YOUTUBE_URI = "http://www.youtube.com/watch?v=";

    @Bind(R.id.movie_detail_overview_text_view)
    TextView overviewTextView;

    @Bind(R.id.movie_detail_title_text_view)
    TextView titleTextView;

    @Bind(R.id.movie_detail_release_date_text_view)
    TextView releaseDateTextView;

    @Bind(R.id.movie_detail_rating_text_view)
    TextView ratingTextView;

    @Bind(R.id.movie_detail_favourite_image_button)
    ImageButton favouriteButton;

    @Bind(R.id.movie_detail_poster_image_view)
    ImageView posterImageView;

    @Bind(R.id.movie_detail_videos_header)
    TextView videosHeader;

    @Bind(R.id.movie_detail_videos_container)
    LinearLayout videosContainer;

    @Bind(R.id.movie_detail_reviews_container)
    LinearLayout reviewsContainer;

    @Bind(R.id.movie_detail_reviews_header)
    TextView reviewsHeader;

    private Movie mMovie;
    private MovieApi mTmdbApi;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.detail_fragment, container, false);

        ButterKnife.bind(this, view);

        Intent intent = getActivity().getIntent();
        getActivity().setTitle("MovieDetail");

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(MovieApi.MOVIEAPI_ENDPOINT)
                .setConverter(Utility.getGsonConverter())
                .build();

        mTmdbApi = restAdapter.create(MovieApi.class);

        int movieId;
        Bundle arguments = getArguments();
        if (arguments != null) {
            movieId = arguments.getInt(MainFragment.EXTRA_MOVIE_ID);
        } else {
            movieId = intent.getIntExtra(MainFragment.EXTRA_MOVIE_ID, -1);
        }

        mMovie = new Select()
                .from(Movie.class)
                .where("external_id = ?", movieId)
                .executeSingle();

        populateFields();
        setFavoriteButtonListener();
        loadReviews();
        fetchTrailers();
        return view;
    }

    private void populateFields() {
        Picasso.with(getActivity())
                .load(mMovie.getPosterPath())
                .into(posterImageView);

        titleTextView.setText(mMovie.getOriginalTitle());
        if(mMovie.getReleaseDate() != null){
            releaseDateTextView.setText(mMovie.getReleaseDate().toString().substring(mMovie.getReleaseDate().toString().length()-4, mMovie.getReleaseDate().toString().length()));
        }
        else{
            releaseDateTextView.setText("");
        }
        overviewTextView.setText(mMovie.getOverview());
        ratingTextView.setText(Float.toString(mMovie.getVoteAverage()) + "/10");

        updateFavoriteStar();
    }

    private void updateFavoriteStar() {
        if (mMovie.getIsFavourite()) {
            favouriteButton.setImageResource(R.drawable.fav_marked);
        } else {
            favouriteButton.setImageResource(R.drawable.fav_unmarked);
        }
    }

    private void setFavoriteButtonListener() {
        favouriteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean newValue = !mMovie.getIsFavourite();
                mMovie.setIsFavorite(newValue);
                mMovie.save();
                updateFavoriteStar();
            }

        });
    }

    private void fetchTrailers() {
        mTmdbApi.getVideosForMovie(mMovie.getExternalId(), new Callback<MovieResponse>() {

            @Override
            public void success(MovieResponse movieVideosResponse, Response response) {
                populateVideosList(movieVideosResponse.results);
            }

            @Override
            public void failure(RetrofitError error) {
            }

        });
    }

    private void populateVideosList(List<Video> videos) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        for (int i = 0; i < videos.size(); i++) {
            Video video = videos.get(i);

            View videoView = inflater.inflate(R.layout.video_item, videosContainer, false);
            Button openTrailerButton = ButterKnife.findById(videoView, R.id.trailer_button);

            openTrailerButton.setText(getString(R.string.trailer_button_title, i + 1));
            setViewTrailerListener(openTrailerButton, video);

            videosContainer.addView(videoView);
        }
    }

    private void setViewTrailerListener(Button videoButton, final Video video) {
        videoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri youtubeUrl = Uri.parse(YOUTUBE_URI + video.key);
                Intent intent = new Intent(Intent.ACTION_VIEW, youtubeUrl);
                startActivity(intent);
            }

        });
    }

    private void loadReviews() {
        mTmdbApi.getReviewsForMovie(mMovie.getExternalId(), new Callback<ReviewResponse>() {

            @Override
            public void success(ReviewResponse reviewResponse, Response response) {
                List<Review> reviews = reviewResponse.results;
                populateReviewsList(reviews);
            }

            @Override
            public void failure(RetrofitError error) {
            }

        });
    }

    private void populateReviewsList(List<Review> reviews) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        for (Review review : reviews) {
            View reviewView = inflater.inflate(R.layout.review_item, reviewsContainer, false);
            TextView authorTextView = ButterKnife.findById(reviewView, R.id.movie_review_author);
            authorTextView.setText(review.author);
            TextView contentTextView = ButterKnife.findById(reviewView, R.id.movie_review);
            contentTextView.setText(review.content);
            reviewsContainer.addView(reviewView);
        }

        if (reviews.isEmpty()) reviewsHeader.setVisibility(View.INVISIBLE);
    }

}
