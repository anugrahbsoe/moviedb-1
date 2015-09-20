package net.sleepbug.moviedb;

/**
 * Created by panzertax on 13/09/15.
 */

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;



import net.sleepbug.moviedb.data.Movie;

public class MainActivity extends AppCompatActivity implements MainFragment.ClickCallback {

    private static final String MOVIE_DETAIL_FRAGMENT_TAG = "MDF_TAG";
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        mTwoPane = findViewById(R.id.movie_detail_container) != null;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onMovieCardClicked(Movie movie) {
        Bundle arguments = new Bundle();
        arguments.putInt(MainFragment.EXTRA_MOVIE_ID, movie.getExternalId());

        if (mTwoPane) {
            Fragment newDetailFragment = new DetailFragment();
            newDetailFragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container, newDetailFragment, MOVIE_DETAIL_FRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(MainFragment.EXTRA_MOVIE_ID, movie.getExternalId());
            startActivity(intent);
        }
    }
}
