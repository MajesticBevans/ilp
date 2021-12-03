package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.mapbox.geojson.Point;

import java.net.http.HttpRequest;
import java.util.ArrayList;

public class LocationConversion
{
    public static ArrayList<LongLat> wordsToLongLats(ArrayList<String> w3ws)
    {
        ArrayList<LongLat> deliveryPoints = new ArrayList<>();
        for (String w3w : w3ws)
        {
            LongLat point = w3wToLongLat(w3w);
            deliveryPoints.add(point);
        }

        return deliveryPoints;
    }


    public static LongLat w3wToLongLat(String w3w)
    {
        Gson gson = new Gson();

        String[] words = w3w.split("\\.");

        HttpRequest request = App.webServer.buildServerRequest("/words/" + words[0] +
                                                                    "/" + words[1] +
                                                                    "/" + words[2] +
                                                                    "/details.json");

        String response = App.webServer.getStringResponse(request);
        W3WDetails details = gson.fromJson(response, W3WDetails.class);

        return new LongLat(details.coordinates.getLng(), details.coordinates.getLat());
    }

    public static LongLat pointToLongLat(Point point)
    {
        return new LongLat(point.longitude(), point.latitude());
    }
}
