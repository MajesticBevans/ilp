package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.mapbox.geojson.*;
import java.io.FileWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Objects;

/**
 * This class contains the main method within which all functionality is contained, and which starts the program
 * execution. It also contains helper functions to support comparison of the program's performance on the current date
 * to others.
 */
public class App
{
    private final static String MACHINE = "localhost";
    private static long startTime;
    public static SQLClient database;
    public static WebClient webServer;

    private static ArrayList<Order> orders = new ArrayList<>();
    private static final ArrayList<LongLat> landmarks = new ArrayList<>();
    private static final ArrayList<Polygon> noFlyZones = new ArrayList<>();

    /**
     * The main method of the program, and the initial method called. Processes the command line arguments and decides
     * the order of execution for the program.
     * @param args the command line arguments passed to the program
     */
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
        try
        {
            Date sqlDate = Date.valueOf(year + "-" + month + "-" + day);
            orders = database.retrieveOrders(sqlDate);
        } catch (Exception e)
        {
            System.err.println("Invalid date input. Please check your values and try again.");
            System.exit(1);
        }

        if (orders.size() == 0)
        {
            System.err.println("No orders found relating to date: " + day + "-" + month + "-" + year);
            System.err.println("Path cannot be calculated");
            System.exit(1);
        }

        database.retrieveOrderDetails(orders, webServer.getMenus());

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

        Flight flight = new Flight(orders, noFlyZones);
        if (!writeGeoJSONFile(outputFileName, flight.generateFlightPath()))
        {
            System.err.println("GeoJSON file writing failed");
        }
        else
        {
            System.out.println("GeoJSON file written successfully");
        }

        System.out.println("Analysis of flight for " + day + "-" + month + "-" + year);
        performanceAnalysis(flight);
    }

    /**
     * Retrieves all the information from the 'buildings' folder of the web server and stores it.
     */
    public static void retrieveBuildingInfo()
    {
        String landmarksJson =
                webServer.getStringResponse(webServer.buildServerRequest("/buildings/landmarks.geojson"));
        String noFlyJson =
                webServer.getStringResponse(webServer.buildServerRequest("/buildings/no-fly-zones.geojson"));

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

    /**
     * Method to create the output GeoJSON file if it does not exist, or to overwrite the file if it does.
     * @param filename the name of the file to create
     * @param path the geoJSON string that represents the flightpath
     * @return true if file written successfully, false otherwise
     */
    private static boolean writeGeoJSONFile(String filename, String path)
    {
        try
        {
            FileWriter fileWriter = new FileWriter(filename, false);
            fileWriter.write(path);
            fileWriter.close();
            return true;
        } catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Prints information about program performance. Namely, the approximate runtime of the program, the number of moves
     * the drone made, and the percentage monetary value.
     * @param flight the flight object representing the drone flight
     */
    private static void performanceAnalysis(Flight flight)
    {
        long timeDiff = System.nanoTime() - startTime;
        System.out.println("Runtime (approx.): " + (float)timeDiff / 1E9f + " seconds");
        System.out.println("Moves: " + flight.getMoveCount());
        System.out.println("Percentage monetary value: " +
                flight.totalDeliveredOrderCost * 100 / Order.totalPlacedOrderCost +
                "%");
    }

    /**
     * A helper function used during testing that prints the flightpath table to the console.
     */
    public static void printFlightpath()
    {
        ArrayList<String> path = database.getFlightpathTable();
        for (String node: path)
        {
            System.out.println(node);
        }
    }
}
