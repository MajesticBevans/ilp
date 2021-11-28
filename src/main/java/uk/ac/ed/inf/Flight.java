package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.util.ArrayList;
import java.util.List;

public class Flight
{
    private final ArrayList<Order> orders;
    private final ArrayList<LongLat> landmarks;
    private final ArrayList<Polygon> noFlyZones;
    private final LongLat APPLETON_TOWER = new LongLat(-3.186874, 55.944494);
    private ArrayList<Point> pointsForLineString = new ArrayList<>();
    private int moveCount;


    public Flight(ArrayList<Order> orders, ArrayList<LongLat> landmarks, ArrayList<Polygon> noFlyZones)
    {
        this.orders = orders;
        this.landmarks = landmarks;
        this.noFlyZones = noFlyZones;
    }


    /**
     * A basic algorithm to find the shortest possible path to complete all the day's deliveries
     * @return
     */
    public String joinTheDots()
    {
        LongLat previousLocation = APPLETON_TOWER;
        for (int i = 0; i < orders.size(); i++)
        {
            ArrayList<LongLat> orderPath = new ArrayList<>();

            for (LongLat pickup: orders.get(i).getPickupLocations())
            {
                orderPath.addAll(straightLineFromTo(previousLocation, pickup));
                previousLocation = pickup;
                orderPath.add(previousLocation); // adds pickup location again to represent the hover move in path
            }

            LongLat deliveryLocation = orders.get(i).getDeliveryLocation();

            orderPath.addAll(straightLineFromTo(previousLocation, deliveryLocation));
            previousLocation = deliveryLocation;
            orderPath.add(previousLocation); // adds delivery location again to represent hover move in path

            //return to appleton on final loop
            if (i == orders.size() - 1) { orderPath.addAll(straightLineFromTo(previousLocation, APPLETON_TOWER)); }

            commitDeliveryPath(orders.get(i), orderPath);
        }
        return FeatureCollection.fromFeature(
                Feature.fromGeometry(
                        (Geometry)LineString.fromLngLats(pointsForLineString))).toJson();
    }

    /**
     * Method that finds the straightest line possible (given the restriction of angles being multiples of 10 only)
     * between two points, ignoring no-fly-zones,
     * and returns the drone path between the two as an ArrayList of LongLat objects.
     * @param origin the starting point of the path
     * @param destination the final point of the path
     * @return the straightest path between the origin and destination inclusive.
     */
    private ArrayList<LongLat> straightLineFromTo(LongLat origin, LongLat destination)
    {
        ArrayList<LongLat> straightPath = new ArrayList<>();
        straightPath.add(origin);
        LongLat previous = origin;

        while (!previous.closeTo(destination))
        {
            previous = previous.nextPosition(previous.angleTo(destination));
            assert previous.isConfined();
            straightPath.add(previous);
            System.out.println("pos: (" + previous.getLongitude() + ", " + previous.getLatitude() + ") dest: (" + destination.getLongitude() + ", " + destination.getLatitude() + ")");
        }
        return straightPath;
    }

    /**
     * Method to write and commit one delivery path (including pickup and delivery) into the database
     * @param order the order to which this delivery path relates
     * @param path the delivery path
     */
    private void commitDeliveryPath(Order order, ArrayList<LongLat> path)
    {
        //write to database
        App.database.writeToDeliveriesTable(order.getOrderNo(),
                                            order.getW3wDeliveryLocation(),
                                            order.getTotalDeliveryCost());
        //TODO
        //boolean canGetBackToAppleton =

        if (moveCount + path.size() - 1 < 1500)
        moveCount += App.database.writeToFlightpathTable(order.getOrderNo(), path);

        //update geoJson temporary list
        for (LongLat node: path)
        {
            assert node.isConfined();
            pointsForLineString.add(Point.fromLngLat(node.getLongitude(), node.getLatitude()));
        }
    }

    public int getMoveCount() { return moveCount; }
}
