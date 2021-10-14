package uk.ac.ed.inf;
import com.google.gson.Gson;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class Menus
{
    private static final HttpClient client = HttpClient.newHttpClient();
    private Gson gson;
    String machine;
    String port;
    String menus;


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
                System.out.println("Connection established");
                menus = response.body();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public int getDeliveryCost(String ... items)
    {
        int cost = 50;

        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
        ArrayList<Shop> shops = gson.fromJson(menus, listType);

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
