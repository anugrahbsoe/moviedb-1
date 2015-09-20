package net.sleepbug.moviedb;

/**
 * Created by panzertax on 13/09/15.
 */
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.activeandroid.query.Select;
import com.squareup.picasso.Picasso;

import net.sleepbug.moviedb.data.Movie;
import net.sleepbug.moviedb.data.Review;
import net.sleepbug.moviedb.data.Video;
import net.sleepbug.moviedb.responses.MovieResponse;
import net.sleepbug.moviedb.responses.ReviewResponse;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

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
    private MovieApi movieApi;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.detail_fragment, container, false);

        ButterKnife.bind(this, view);

        Intent intent = getActivity().getIntent();
        getActivity().setTitle("MovieDetail");
        setHasOptionsMenu(true);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(MovieApi.MOVIEAPI_ENDPOINT)
                .setConverter(Utility.getGsonConverter())
                .build();

        movieApi = restAdapter.create(MovieApi.class);

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


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case R.id.menu_item_share:
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = "You should watch the movie "+ mMovie.getOriginalTitle()+". It rocks!";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Amazing movie");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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
        movieApi.getVideosForMovie(mMovie.getExternalId(), new Callback<MovieResponse>() {

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
        movieApi.getReviewsForMovie(mMovie.getExternalId(), new Callback<ReviewResponse>() {

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
