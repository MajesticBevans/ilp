package uk.ac.ed.inf;
import com.google.gson.Gson;

import java.net.URI;
import java.util.ArrayList;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * Class to contain data on shops and their menus, and to perform the calculations associated with this data.
 */
public class Menus
{
    private static final HttpClient client = HttpClient.newHttpClient();
    String machine;
    String port;
    String menus;

    /**
     * Class constructor. Establishes connection to the http web server and retrieves the menus.json file relevant to
     * this class.
     * @param machine the name of the machine on which the web server is running
     * @param port the port on which the web server is running
     */
    public Menus(String machine, String port)
    {
        this.machine = machine;
        this.port = port;

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create("http://" + machine + ":"+ port + "/menus/menus.json"))
                                         .build();
        try
        {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200)
            {
                System.out.println("HTTP request failed with status code: " + response.statusCode());
                System.exit(1);
            }
            else
            {
                menus = response.body();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Calculates the total cost of a delivery based on which items have been ordered.
     * @param items a variable number of strings containing the names of the items ordered
     * @return the cost of all the ordered items + the 50p delivery fee, in pence
     */
    public int getDeliveryCost(String ... items)
    {
        int cost = 50;

        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
        ArrayList<Shop> shops = new Gson().fromJson(menus, listType);

        for (Shop shop : shops)
        {
            for (String item : items)
            {
                int pence = shop.getPence(item);
                if (pence != -1)
                {
                    cost += pence;
                }
            }
        }

        return cost;
    }
}
