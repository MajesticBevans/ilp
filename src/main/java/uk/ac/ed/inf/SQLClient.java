package uk.ac.ed.inf;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SQLClient
{
    private final String MACHINE;
    private final String PORT;
    private final String JDBC_STRING;

    public SQLClient(String machine, String port)
    {
        this.MACHINE = machine;
        this.PORT = port;
        this.JDBC_STRING = buildJDBCString();
    }

    public HashMap<String, String> retrieveOrders(String day, String month, String year)
    {
        //convert date into sql format
        Date sqlDate = Date.valueOf(year + "-" + month + "-" + day);
        Connection conn = getDatabaseConnection();
        String ordersQuery = "select * from orders where deliveryDate=(?)";
        try
        {
            PreparedStatement psOrdersQuery = conn.prepareStatement(ordersQuery);
            psOrdersQuery.setDate(1, sqlDate);

            HashMap<String, String> ordersAndLocations = new HashMap<>();
            ResultSet resultSet = psOrdersQuery.executeQuery();

            while (resultSet.next())
            {
                String order = resultSet.getString("orderNo");
                String w3wLocation = resultSet.getString("deliverTo");
                ordersAndLocations.put(order, w3wLocation);
            }

            return ordersAndLocations;
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        assert false;
        return null;
    }

    private Connection getDatabaseConnection()
    {
        try
        {
            return DriverManager.getConnection(JDBC_STRING);
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        assert false;
        return null;
    }

    private String buildJDBCString()
    {
        final String PROTOCOL = "jdbc:derby";
        final String DATABASE_NAME = "/derbyDB";

        return PROTOCOL + "://" +
                MACHINE + ":" +
                PORT +
                DATABASE_NAME;
    }
}
