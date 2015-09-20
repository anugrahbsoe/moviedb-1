package net.sleepbug.moviedb;

/**
 * Created by panzertax on 13/09/15.
 */
import com.activeandroid.query.Select;
import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import com.activeandroid.ActiveAndroid;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import net.sleepbug.moviedb.data.Movie;
import net.sleepbug.moviedb.responses.MoviesResponse;
import android.view.MenuItem;
import java.util.List;
import butterknife.Bind;
import butterknife.ButterKnife;


public class MainFragment extends Fragment implements MovieAdapter.ClickListener {

    public static final String EXTRA_MOVIE_ID = "net.sleepbug.moviedb.EXTRA_MOVIE_ID";
    public static final String PREFERENCE_FILE = "MovieDBFile";

    private static final String ORDER_POPULARITY_DESC = "popularity.desc";
    private static final String ORDER_RATING_DESC = "vote_average.desc";
    private static final String FILTER_FAVORITES = "favorites";
    private static final String PREF_SORT_ORDER = "sort_order";

    private MovieAdapter mAdapter;
    private MovieApi movieApi;
    private String mCurrentOrder;
    private List<Movie> mMoviesList;
    private RecyclerView.LayoutManager mLayoutManager;

    @Bind(R.id.movies_recycler_view)
    RecyclerView moviesRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment, container, false);

        ButterKnife.bind(this, view);

        setHasOptionsMenu(true);

        mLayoutManager = new GridLayoutManager(getActivity(), 2);
        moviesRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new MovieAdapter();
        moviesRecyclerView.setAdapter(mAdapter);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setConverter(Utility.getGsonConverter())
                .setEndpoint(MovieApi.MOVIEAPI_ENDPOINT)
                .build();

        movieApi = restAdapter.create(MovieApi.class);
        initializeMoviesList();

        mAdapter.setClickListener(this);

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        SharedPreferences preferences = getActivity().getSharedPreferences(PREFERENCE_FILE, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        switch (itemId) {
            case R.id.action_sort_popularity:
                listPopularMovies();

                mCurrentOrder = ORDER_POPULARITY_DESC;
                editor.putString(PREF_SORT_ORDER, mCurrentOrder);
                editor.apply();
                return true;

            case R.id.action_sort_rating:
                listHighestRatedMovies();

                mCurrentOrder = ORDER_RATING_DESC;
                editor.putString(PREF_SORT_ORDER, mCurrentOrder);
                editor.apply();
                return true;

            case R.id.action_filter_favorites:
                listFavoriteMovies();

                editor.putString(PREF_SORT_ORDER, FILTER_FAVORITES);
                editor.apply();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMovieCardClicked(View view, Movie movie) {
        ClickCallback callback = (ClickCallback) getActivity();
        callback.onMovieCardClicked(movie);
    }

    private void listPopularMovies() {
        loadCachedMovies(ORDER_POPULARITY_DESC);
        getActivity().setTitle(R.string.most_popular);

        if (mMoviesList.isEmpty()) updateMoviesFromApi(ORDER_POPULARITY_DESC);
    }

    private void listHighestRatedMovies() {
        loadCachedMovies(ORDER_RATING_DESC);
        getActivity().setTitle(R.string.highest_rating);

        if (mMoviesList.isEmpty()) updateMoviesFromApi(ORDER_RATING_DESC);
    }

    private void listFavoriteMovies() {
        getActivity().setTitle(R.string.favorites);

        mMoviesList = new Select()
                .from(Movie.class)
                .where("is_favorite = ?", true)
                .execute();

        mAdapter.setDataset(mMoviesList);
    }

    private void initializeMoviesList() {
        SharedPreferences preferences = getActivity().getSharedPreferences(PREFERENCE_FILE, Activity.MODE_PRIVATE);
        mCurrentOrder = preferences.getString(PREF_SORT_ORDER, ORDER_POPULARITY_DESC);

        switch (mCurrentOrder) {
            case ORDER_POPULARITY_DESC:
                listPopularMovies();
                break;
            case ORDER_RATING_DESC:
                listHighestRatedMovies();
                break;
            case FILTER_FAVORITES:
                listFavoriteMovies();
                break;
        }
    }

    private void loadCachedMovies(String order) {
        String orderBy;
        if (order.equals(ORDER_POPULARITY_DESC)) {
            orderBy = "popularity DESC";
        } else {
            orderBy = "vote_average DESC";
        }

        mMoviesList = new Select()
                .from(Movie.class)
                .orderBy(orderBy)
                .execute();

        mAdapter.setDataset(mMoviesList);
    }

    private void updateMoviesFromApi(String order) {
        movieApi.getMovies(order, new Callback<MoviesResponse>() {

            @Override
            public void success(MoviesResponse responseObject, Response response) {
                mMoviesList = responseObject.results;
                mAdapter.setDataset(mMoviesList);
                mLayoutManager.scrollToPosition(0);
                persistMovies(mMoviesList);
            }

            @Override
            public void failure(RetrofitError error) {
            }
        });
    }

    private void persistMovies(List<Movie> moviesFromApi) {
        ActiveAndroid.beginTransaction();

        try {
            for (Movie movieFromApi : moviesFromApi) {
                Movie movieFromDb = new Select()
                        .from(Movie.class)
                        .where("external_id = ?", movieFromApi.getExternalId())
                        .executeSingle();

                if (movieFromDb == null) {
                    movieFromApi.save();
                    continue;
                }

                movieFromDb.setExternalId(movieFromApi.getExternalId());
                movieFromDb.setTitle(movieFromApi.getTitle());
                movieFromDb.setOriginalTitle(movieFromApi.getOriginalTitle());
                movieFromDb.setPosterPath(movieFromApi.getPosterPath());
                movieFromDb.setBackdropPath(movieFromApi.getBackdropPath());
                movieFromDb.setOverview(movieFromApi.getOverview());
                movieFromDb.setPopularity(movieFromApi.getPopularity());
                movieFromDb.setVoteAverage(movieFromApi.getVoteAverage());
                movieFromDb.setReleaseDate(movieFromApi.getReleaseDate());

                movieFromDb.save();
            }

            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
    }

    public interface ClickCallback {

        void onMovieCardClicked(Movie movie);

    }
}
