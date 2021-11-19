package uk.ac.ed.inf;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SQLClient
{
    private final String MACHINE;
    private final String PORT;
    private final String JDBC_STRING;

    /**
     * Class constructor. Stores the machine name and port number, and calls buildJDBCString.
     * @param machine the machine name
     * @param port the port number
     */
    public SQLClient(String machine, String port)
    {
        this.MACHINE = machine;
        this.PORT = port;
        this.JDBC_STRING = buildJDBCString();
    }

    public ArrayList<Order> retrieveOrdersAndDeliveryLocations(String day, String month, String year)
    {
        //convert date into sql format
        Date sqlDate = Date.valueOf(year + "-" + month + "-" + day);

        Connection conn = getDatabaseConnection();
        String ordersQuery = "select * from orders where deliveryDate=(?)";

        try
        {
            PreparedStatement psOrdersQuery = conn.prepareStatement(ordersQuery);
            psOrdersQuery.setDate(1, sqlDate);

            ArrayList<Order> orders = new ArrayList<>();
            ResultSet resultSet = psOrdersQuery.executeQuery();
            System.out.println("Orders query successful");

            while (resultSet.next())
            {
                String orderNo = resultSet.getString("orderNo");
                String w3wLocation = resultSet.getString("deliverTo");
                orders.add(new Order(orderNo, w3wLocation));
            }
            return orders;
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        assert false;
        return null;
    }

    public void retrieveAndSetPickupLocations(ArrayList<Order> orders, ArrayList<Shop> menus)
    {
        Connection conn = getDatabaseConnection();
        String orderDetailsQuery = "select * from orderDetails where orderNo=(?)";
        for (Order order: orders)
        {
            try
            {
                PreparedStatement psOrderDetailsQuery = conn.prepareStatement(orderDetailsQuery);
                psOrderDetailsQuery.setString(1, order.getOrderNo());
                ArrayList<String> items = new ArrayList<>();
                ResultSet resultSet = psOrderDetailsQuery.executeQuery();

                while (resultSet.next())
                {
                    items.add(resultSet.getString("item"));
                }

                order.setOrderDetails(items, menus);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to establish a connection to the Derby database
     * @return an SQL connection over which to execute statements
     */
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

    /**
     * Method to build a full JDBC string when the machine name and port number have been established
     * @return the JDBC string
     */
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
