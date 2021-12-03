package uk.ac.ed.inf;
import com.google.gson.Gson;
import com.mapbox.geojson.*;

import java.io.FileWriter;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Objects;

public class App
{
    private final static String MACHINE = "localhost";
    private static long startTime;
    public static SQLClient database;
    public static WebClient webServer;

    private static ArrayList<Order> orders = new ArrayList<>();
    private static ArrayList<LongLat> landmarks = new ArrayList<>();
    private static ArrayList<Polygon> noFlyZones = new ArrayList<>();
    private static ArrayList<Shop> menus;

    public static void main(String[] args)
    {

        startTime = System.nanoTime();

        String day = args[0];
        String month = args[1];
        String year = args[2];
        String serverPort = args[3];
        String databasePort = args[4];

        String outputFileName = "drone-" +
                day + "-" +
                month + "-" +
                year + ".geojson";

        //create the server client to be used for this run
        webServer = new WebClient(MACHINE, serverPort);

        //create the sql client to be used for this run
        database = new SQLClient(MACHINE, databasePort);

        //getFlightpath();
        //System.exit(1);
        //fetch all the order information from the database and web server
        orders = database.retrieveOrders(day, month, year);
        menus = webServer.getMenus();
        database.retrieveOrderDetails(orders, menus);

        //create the output database tables
        database.createTable("deliveries",
                "orderNo char(8)",
                "deliveredTo varchar(19)",
                "costInPence int");

        database.createTable("flightpath",
                "orderNo char(8)",
                "fromLongitude double",
                "fromLatitude double",
                "angle integer",
                "toLongitude double",
                "toLatitude double");

        //retrieve the landmark and no-fly zone locations
        retrieveBuildingInfo();

        Flight flight = new Flight(orders, landmarks, noFlyZones);
        String geoJsonPath = flight.generateFlightPath();
        try
        {
            FileWriter fileWriter = new FileWriter(outputFileName, false);
            fileWriter.append(geoJsonPath);
            fileWriter.close();
            System.out.println("GeoJSON file created");
        } catch (Exception e)
        {
            System.err.println("GeoJSON file cannot be created");
            e.printStackTrace();
        }

        System.out.println("Analysis of flight for " + day + "-" + month + "-" + year);
        performanceAnalysis(flight);

        //TODO

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
        for (Feature feat : Objects.requireNonNull(FeatureCollection.fromJson(landmarksJson).features()))
        {
            Point point = (Point)feat.geometry();
            assert point != null;
            landmarks.add(new LongLat(point.longitude(), point.latitude()));
        }

        for (Feature feat : Objects.requireNonNull(FeatureCollection.fromJson(noFlyJson).features()))
        {
            Polygon polygon = (Polygon)feat.geometry();
            noFlyZones.add(polygon);
        }
    }

    private static void performanceAnalysis(Flight flight)
    {
        long timeDiff = System.nanoTime() - startTime;
        System.out.println("Runtime (approx.): " + (float)timeDiff / 1E9f + " seconds");
        System.out.println("Moves: " + flight.getMoveCount());
        System.out.println("Percentage monetary value: " +
                flight.totalDeliveredOrderCost * 100 / Order.totalPlacedOrderCost +
                "%");
    }

    public static void getFlightpath()
    {
        ArrayList<String> path = database.getFlightpathTable();
        for (String node: path)
        {
            System.out.println(node);
        }
    }
}
