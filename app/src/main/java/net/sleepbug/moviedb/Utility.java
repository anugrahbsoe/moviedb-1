package net.sleepbug.moviedb;

/**
 * Created by panzertax on 13/09/15.
 */

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.reflect.Type;
import retrofit.converter.GsonConverter;

public class Utility {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private static GsonConverter sGsonConverter;

    public static GsonConverter getGsonConverter() {
        if (sGsonConverter != null) return sGsonConverter;

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new JsonDeserializer() {
                    DateFormat df = new SimpleDateFormat(DATE_FORMAT);

                    @Override
                    public Date deserialize(final JsonElement json, final Type typeOfT,
                                            final JsonDeserializationContext context)
                            throws JsonParseException {
                        try {
                            return df.parse(json.getAsString());
                        } catch (ParseException e) {
                            return null;
                        }
                    }
                })
                .create();

        sGsonConverter = new GsonConverter(gson);
        return sGsonConverter;
    }

    private Utility() {}
}
