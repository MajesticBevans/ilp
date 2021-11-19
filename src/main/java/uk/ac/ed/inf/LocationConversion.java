package uk.ac.ed.inf;

import com.google.gson.Gson;
import java.net.http.HttpRequest;
import java.util.ArrayList;

public class LocationConversion
{
    public static ArrayList<LongLat> wordsToLongLats(WebClient client, ArrayList<String> w3ws)
    {
        ArrayList<LongLat> deliveryPoints = new ArrayList<>();
        for (String w3w : w3ws)
        {
            LongLat point = w3wToLongLat(client, w3w);
            deliveryPoints.add(point);
        }

        return deliveryPoints;
    }


    public static LongLat w3wToLongLat(WebClient client, String w3w)
    {
        Gson gson = new Gson();

        String[] words = w3w.split("\\.");

        HttpRequest request = client.buildServerRequest("/words/" + words[0] +
                                                                    "/" + words[1] +
                                                                    "/" + words[2] +
                                                                    "/details.json");

        String response = client.getStringResponse(request);
        W3WDetails details = gson.fromJson(response, W3WDetails.class);

        return new LongLat(details.coordinates.getLng(), details.coordinates.getLat());
    }
}
