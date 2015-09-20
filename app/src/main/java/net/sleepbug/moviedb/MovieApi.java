package net.sleepbug.moviedb;

/**
 * Created by panzertax on 13/09/15.
 */

import net.sleepbug.moviedb.responses.*;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.Callback;

public interface MovieApi {

    String MOVIEAPI_ENDPOINT = "http://api.themoviedb.org/3";
    String MOVIEAPI_KEY = "87181ff74d818cf8ffa3be895f249109";

    @GET("/discover/movie?api_key=" + MOVIEAPI_KEY)
    void getMovies(@Query("sort_by") String order, Callback<MoviesResponse> cb);

    @GET("/movie/{id}/videos?api_key=" + MOVIEAPI_KEY)
    void getVideosForMovie(@Path("id") int movieId, Callback<MovieResponse> cb);

    @GET("/movie/{id}/reviews?api_key=" + MOVIEAPI_KEY)
    void getReviewsForMovie(@Path("id") int movieId, Callback<ReviewResponse> cb);

}
