package uk.ac.ed.inf;

import java.sql.*;
import java.util.ArrayList;

public class SQLClient
{
    private final String MACHINE;
    private final String PORT;
    private final String JDBC_STRING;
    private final Connection CONN;
    private final String CONNECTION_ERROR_MESSAGE =
            "Database connection failed. Check database is running and port number is correct.";

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
        this.CONN = getDatabaseConnection();
    }

    public ArrayList<Order> retrieveOrders(String day, String month, String year)
    {
        //convert date into sql format
        Date sqlDate = Date.valueOf(year + "-" + month + "-" + day);
        String ordersQuery = "select * from orders where deliveryDate=(?)";

        try
        {
            PreparedStatement psOrdersQuery = CONN.prepareStatement(ordersQuery);
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


    /**
     * Creates a table in the database with a specified name, column headings and datatypes.
     * @param tableName the table title
     * @param columns a variable number of strings that contain the column heading and datatype
     */
    public void createTable(String tableName, String ... columns)
    {
        dropTableIfExists(tableName);
        StringBuilder str = new StringBuilder("create table " + tableName + "(");
        for (String column: columns)
        {
            str.append(column).append(", ");
        }
        str.delete(str.length() - 2, str.length());
        str.append(")");

        try
        {
            Statement statement = CONN.createStatement();
            statement.execute(str.toString());
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Checks if a table with the specified name exists within the database, and if it does, it is removed.
     * @param tableName the name of the table to be dropped
     */
    private void dropTableIfExists(String tableName)
    {
        try
        {
            DatabaseMetaData databaseMetaData = CONN.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null,
                    null,
                    tableName.toUpperCase(),
                    null);
            if (resultSet.next())
            {
                Statement drop_statement = CONN.createStatement();
                drop_statement.execute("drop table " + tableName);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void writeToDeliveriesTable(String orderNo, String deliveredTo, int costInPence)
    {
        try
        {
            PreparedStatement psDeliveries = CONN.prepareStatement("insert into deliveries values (?,?,?)");

            psDeliveries.setString(1, orderNo);
            psDeliveries.setString(2, deliveredTo);
            psDeliveries.setInt(3, costInPence);
            psDeliveries.execute();
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public int writeToFlightpathTable(String orderNo, ArrayList<LongLat> path)
    {
        int moveCount = 0;
        try
        {
            PreparedStatement psFlightpath = CONN.prepareStatement("insert into flightpath values (?,?,?,?,?,?)");

            for (int i = 0; i < path.size() - 1; i++)
            {
                psFlightpath.setString(1, orderNo);
                psFlightpath.setDouble(2, path.get(i).getLongitude());
                psFlightpath.setDouble(3, path.get(i).getLatitude());
                psFlightpath.setInt(4, path.get(i).angleTo(path.get(i + 1)));
                psFlightpath.setDouble(5, path.get(i + 1).getLongitude());
                psFlightpath.setDouble(6, path.get(i + 1).getLatitude());
                psFlightpath.execute();
                moveCount++;
            }
            return moveCount;
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        assert false;
        return 0;
    }

    public void retrieveOrderDetails(ArrayList<Order> orders, ArrayList<Shop> menus)
    {
        String orderDetailsQuery = "select * from orderDetails where orderNo=(?)";
        for (Order order: orders)
        {
            try
            {
                PreparedStatement psOrderDetailsQuery = CONN.prepareStatement(orderDetailsQuery);
                psOrderDetailsQuery.setString(1, order.getOrderNo());
                ArrayList<String> items = new ArrayList<>();
                ResultSet resultSet = psOrderDetailsQuery.executeQuery();

                while (resultSet.next())
                {
                    items.add(resultSet.getString("item"));
                }

                order.setOrderDetails(items, menus);
                Order.totalPlacedOrderCost += order.getTotalCost();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<String> getFlightpathTable()
    {
        ArrayList<String> path = new ArrayList<>();
        String flightpathQuery = "select * from flightpath";
        try
        {
            Statement query = CONN.createStatement();
            ResultSet resultSet = query.executeQuery(flightpathQuery);

            while (resultSet.next())
            {
                String str = "OrderNo: " + resultSet.getString("orderNo") +
                        " toLongitude: " +
                        resultSet.getDouble("toLongitude") +
                        " angle: " +
                        resultSet.getInt("angle") +
                        " toLatitude: " +
                        resultSet.getDouble("toLatitude");
                path.add(str);
            }
            return path;
        } catch (Exception e)
        {
            return null;
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
            System.err.println(CONNECTION_ERROR_MESSAGE);
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
