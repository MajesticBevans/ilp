package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.mapbox.geojson.Point;

import java.net.http.HttpRequest;
import java.util.ArrayList;

/**
 * Class with exclusively static methods that is used to convert locations between the different formats used in this
 * program.
 */
public class LocationConversion
{
    /**
     * Method to convert a list of what3words strings into longitude and latitude format.
     * @param w3ws the what3words strings
     * @return a list of LongLat objects representing the co-ordinates
     */
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

    /**
     * Method to convert a what3words string into longitude and latitude format.
     * @param w3w the what3words string
     * @return a LongLat object representing the co-ordinates
     */
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

    /**
     * Method to convert a GeoJSON point into a LongLat object.
     * @param point a GeoJSON point
     * @return a LongLat object with the co-ordinates of the given point.
     */
    public static LongLat pointToLongLat(Point point)
    {
        return new LongLat(point.longitude(), point.latitude());
    }
}
