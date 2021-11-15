package uk.ac.ed.inf;


import java.util.ArrayList;
import java.util.HashMap;

public class App
{
    final static String MACHINE = "localhost";

    private static HashMap<String,String> ordersAndLocations = new HashMap<>();

    public static void main(String[] args)
    {
        String day = args[0];
        String month = args[1];
        String year = args[2];
        String serverPort = args[3];
        String databasePort = args[4];

        //create the sql client to be used for this run
        SQLClient database = new SQLClient(MACHINE, databasePort);

        //create the server client to be used for this run
        WebClient webServer = new WebClient(MACHINE, serverPort);

        ordersAndLocations = database.retrieveOrders(day, month, year);



    }
}
