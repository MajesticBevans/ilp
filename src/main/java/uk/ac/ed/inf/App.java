package uk.ac.ed.inf;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.*;

import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.util.ArrayList;

public class App
{
    final static String MACHINE = "localhost";

    private static ArrayList<Order> orders = new ArrayList<>();

    public static SQLClient database;
    public static WebClient webServer;
    private static ArrayList<LongLat> landmarks = new ArrayList<>();
    private static ArrayList<Polygon> noFlyZones = new ArrayList<>();
    private static ArrayList<Shop> menus;

    public static void main(String[] args)
    {
        String day = args[0];
        String month = args[1];
        String year = args[2];
        String serverPort = args[3];
        String databasePort = args[4];

        //create the sql client to be used for this run
        database = new SQLClient(MACHINE, databasePort);

        //create the server client to be used for this run
        webServer = new WebClient(MACHINE, serverPort);

        //get orders and delivery locations for the given date
        orders = database.retrieveOrdersAndDeliveryLocations(day, month, year);

        setupMenus();

        database.retrieveAndSetPickupLocations(orders, menus);

        //retrieve the landmark and no-fly zone locations
        retrieveBuildingInfo();

        //TODO
        //Build basic flightpath (join the dots, just need to get the order correct)
        //Then make it avoid no-fly zones (ie incorporate the buildings)
        //Finish the functionality, then optimise the flightpath algorithm
    }


    public static void retrieveBuildingInfo()
    {
        HttpRequest landmarksRequest = webServer.buildServerRequest("/buildings/landmarks.geojson");
        HttpRequest noFlyRequest = webServer.buildServerRequest("/buildings/no-fly-zones.geojson");
        String landmarksJson = webServer.getStringResponse(landmarksRequest);
        String noFlyJson = webServer.getStringResponse(noFlyRequest);

        Gson gson = new Gson();

        for (Feature feat : FeatureCollection.fromJson(landmarksJson).features())
        {
            Point point = (Point)feat.geometry();
            landmarks.add(new LongLat(point.longitude(), point.latitude()));
        }

        for (Feature feat : FeatureCollection.fromJson(noFlyJson).features())
        {
            Polygon polygon = (Polygon)feat.geometry();
            noFlyZones.add(polygon);
        }
    }

    private static void setupMenus()
    {
        HttpRequest request = webServer.buildServerRequest("/menus/menus.json");
        String response = webServer.getStringResponse(request);

        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();

        menus = new Gson().fromJson(response, listType);
    }
}
