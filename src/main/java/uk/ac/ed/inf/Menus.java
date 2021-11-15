package uk.ac.ed.inf;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.net.http.HttpRequest;

/**
 * Class to contain data on shops and their menus, and to perform the calculations associated with this data.
 */
public class Menus
{
    String machine;
    String port;
    String menus;

    /**
     * Class constructor. Establishes connection to the http web server and retrieves the menus.json file relevant to
     * this class.
     * @param client an instance of the ServerClient class to allow and support connections to the web server.
     */
    public Menus(WebClient client)
    {
        HttpRequest request = client.buildServerRequest("/menus/menus.json");
        menus = client.getStringResponse(request);
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
