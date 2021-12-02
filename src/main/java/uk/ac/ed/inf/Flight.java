package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.util.ArrayList;
import java.util.List;

public class Flight
{
    private final ArrayList<Order> orders;
    private final ArrayList<LongLat> landmarks;
    private final ArrayList<Polygon> noFlyZones;
    private static final LongLat APPLETON_TOWER = new LongLat(-3.186874, 55.944494);
    private ArrayList<Point> pointsForLineString = new ArrayList<>();
    private int moveCount;
    private static final int MAX_MOVE_COUNT = 1500;
    public int totalDeliveredOrderCost;
    private static final int GREATER_FLAG = -1;
    private static final int LESSER_FLAG = 1;

    /**
     * Class constructor. Simply takes the information needed to calculate a flightpath and stores it.
     * @param orders the list of Order objects relating to the orders placed during this day.
     * @param landmarks the landmarks from /buildings/landmarks.geojson
     * @param noFlyZones the no-fly-zones from /buildings/no-fly-zones.geojson
     */
    public Flight(ArrayList<Order> orders, ArrayList<LongLat> landmarks, ArrayList<Polygon> noFlyZones)
    {
        this.orders = orders;
        this.landmarks = landmarks;
        this.noFlyZones = noFlyZones;
    }

    /**
     * Top-level function that decides the order in which locations are visited during the flightpath, and calls the
     * appropriate methods to generate the sub-paths between them and commit these paths to the database.
     * @return the GeoJSON LineString representing the flightpath
     */
    public String generateFlightPath()
    {
        LongLat previousLocation = APPLETON_TOWER;

        for (Order order : orders)
        {
            ArrayList<LongLat> orderPath = new ArrayList<>();

            for (LongLat pickup : order.getPickupLocations())
            {
                orderPath.addAll(createSubPath(previousLocation, pickup));
                previousLocation = pickup;
                orderPath.add(previousLocation); // adds pickup location again to represent the hover move in path
            }

            LongLat deliveryLocation = order.getDeliveryLocation();

            orderPath.addAll(createSubPath(previousLocation, deliveryLocation));
            previousLocation = deliveryLocation;
            orderPath.add(previousLocation); // adds delivery location again to represent hover move in path

            if (!commitDeliveryPath(order, orderPath))
            {
                return FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(pointsForLineString))).toJson();
            }
        }

        //return to appleton
        commitDeliveryPath(orders.get(orders.size() - 1), createSubPath(previousLocation, APPLETON_TOWER));

        return FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(pointsForLineString))).toJson();
    }

    /**
     * Helper function that returns a straight path between two points if the path doesn't enter any no-fly-zones, or
     * returns the result of avoidNoFlyZones otherwise.
     * @param origin the starting point of the path
     * @param destination the final point of the path
     * @return the path
     */
    private ArrayList<LongLat> createSubPath(LongLat origin, LongLat destination)
    {
        ArrayList<LongLat> subPath = straightLineFromTo(origin, destination);
        if (subPath.size() == 0) { return avoidNoFlyZones(origin, destination); }
        else { return subPath; }
    }

    /**
     * Method that finds the straightest line possible (given the restriction of angles being multiples of 10 only)
     * between two points, ignoring no-fly-zones.
     * @param origin the starting point of the path
     * @param destination the final point of the path
     * @return the straightest path between the origin and destination inclusive
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
            if (isInNo_Fly_Zone(previous)) { return new ArrayList<>(); }

            straightPath.add(previous);
            //System.out.println("pos: (" + previous.getLongitude() + ", " +
              //      previous.getLatitude() + ") dest: (" +
                //    destination.getLongitude() + ", " +
                  //  destination.getLatitude() + ")");
        }
        return straightPath;
    }

    /**
     * Method that finds a path between two points, avoiding no-fly-zones.
     * @param origin the starting point of the path
     * @param destination the final point of the path
     * @return the calculated path between the origin and destination inclusive
     */
    private ArrayList<LongLat> avoidNoFlyZones(LongLat origin, LongLat destination)
    {
        ArrayList<LongLat> path = new ArrayList<>();

        path.add(origin);

        int initAngle = origin.angleTo(destination);
        int greaterAngle = (initAngle + 10) % 360;
        int lesserAngle = (initAngle - 10) % 360;

        ArrayList<LongLat> greaterPath = testPath(origin, destination, greaterAngle);
        ArrayList<LongLat> lesserPath = testPath(origin, destination, lesserAngle);

        while(greaterPath.size() == 0 && lesserPath.size() == 0)
        {
            greaterAngle = (greaterAngle + 10) % 360;
            lesserAngle = (lesserAngle - 10) % 360;

            greaterPath = testPath(origin, destination, greaterAngle);
            lesserPath = testPath(origin, destination, lesserAngle);
        }

        if(greaterPath.size() > 0)
        {
            int turnAngle = (greaterAngle - 10) % 360;
            path.addAll(turnTowardsDestination(greaterPath, destination, turnAngle, GREATER_FLAG));
        }
        else
        {
            int turnAngle = (lesserAngle + 10) % 360;
            path.addAll(turnTowardsDestination(lesserPath, destination, turnAngle, LESSER_FLAG));
        }
        return path;
    }

    /**
     * Helper method for avoidNoFLyZones that, given a straight path that extends to the edge of the confinement zone,
     * tests each node within the path to see if it can be a turning point towards the given destination (without
     * entering a no-fly-zone). If this is the case, it generates a straight path from this node, following as close
     * an angle as possible to the destination, and calls itself recursively using this new path as the input. If a
     * straight path to the destination is available at any stage, this is appended to the path and returned.
     * @param initialPath the straight path extending to the edge of the confinement zone
     * @param destination the final destination that we want to reach
     * @param turnAngle the angle that is tested on each node to see if a turn is valid
     * @param flag a flag value that indicates which direction the path should turn (increase or decrease the angle)
     * @return a path that begins on the initial path, and turns towards the destination until it is reached.
     */
    private ArrayList<LongLat> turnTowardsDestination(ArrayList<LongLat> initialPath, LongLat destination, int turnAngle, int flag)
    {
        ArrayList<LongLat> returnPath = new ArrayList<>();

        for (LongLat node: initialPath)
        {
            ArrayList<LongLat> destinationTestPath = testPath(node, destination, node.angleTo(destination));
            ArrayList<LongLat> turnTestPath = testPath(node, destination, turnAngle);
            if (destinationTestPath.size() > 0)
            {
                returnPath.addAll(straightLineFromTo(node, destination));
                return returnPath;
            }
            else if (turnTestPath.size() > 0)
            {
                turnAngle = (turnAngle + 10 * flag) % 360;
                ArrayList<LongLat> maxTurnPath = testPath(node, destination, turnAngle);

                while (maxTurnPath.size() > 0 &&
                        turnAngle > node.angleTo(destination))
                {
                    turnTestPath = maxTurnPath;
                    turnAngle = (turnAngle + 10 * flag) % 360;
                    maxTurnPath = testPath(node, destination, turnAngle);
                }
                returnPath.addAll(turnTowardsDestination(turnTestPath, destination, turnAngle, flag));
                return returnPath;
            }
            else { returnPath.add(node); }
        }
        assert false;
        return returnPath;
    }

    private ArrayList<LongLat> testPath(LongLat origin, LongLat destination, int angle)
    {
        ArrayList<LongLat> path = new ArrayList<>();
        path.add(origin);
        LongLat next = origin.nextPosition(angle);

        while (next.isConfined() && !next.closeTo(destination))
        {
            if (isInNo_Fly_Zone(next))
            {
                return new ArrayList<>();
            }
            path.add(next);
            next = next.nextPosition(angle);
        }
        if (next.closeTo(destination)) { path.add(next); }
        return path;
    }

    /**
     * Method that checks if a point is within any of the no-fly-zones.
     * @param point the point that is being tested
     * @return true if contained within a no-fly-zone, false otherwise
     */
    private boolean isInNo_Fly_Zone(LongLat point)
    {
        for (Polygon poly: noFlyZones)
        {
            List<Point> co_ords = poly.coordinates().get(0); // second list is for holes within polygon,
            // irrelevant for no fly zones
            int i;
            int j;
            boolean contained = false;

            for (i = 0, j = co_ords.size() - 2; i < co_ords.size() - 1; j = i++) // for loop gets the line between each pair of consecutive points
            {
                double intersectionLongitude = (co_ords.get(j).longitude() - co_ords.get(i).longitude()) *
                        (point.getLatitude() - co_ords.get(i).latitude()) / (co_ords.get(j).latitude() - co_ords.get(i).latitude()) +
                        co_ords.get(i).longitude();

                if ((co_ords.get(i).latitude() > point.getLatitude()) != (co_ords.get(j).latitude() > point.getLatitude()) // first condition checks if the point is between the latitude values of the line endings
                        && (point.getLongitude() < intersectionLongitude))
                {
                    contained = !contained;
                }
            }
            if (contained) { return true; }
        }
        return false;
    }

    /**
     * Method to write and commit one complete delivery path (including pickup and delivery) into the flightpath table
     * and update the deliveries table accordingly. If the drone does not have enough battery power to complete the delivery,
     * it instead returns to Appleton Tower, commits this to the flightpath and returns false.
     * @param order the order to which this delivery path relates
     * @param path the delivery path
     * @return true if delivery path committed, false if return path to Appleton committed
     */
    private boolean commitDeliveryPath(Order order, ArrayList<LongLat> path)
    {
        int newMoveCount = moveCount + path.size() - 1;

        ArrayList<LongLat> pathBackToAppleton = createSubPath(path.get(path.size() - 1), APPLETON_TOWER);
        // boolean that determines whether the drone will have enough battery power to return to appleton if this path is committed
        boolean canGetBackToAppleton = pathBackToAppleton.size() - 1 < MAX_MOVE_COUNT - newMoveCount;

        if (canGetBackToAppleton)
        {
            //write to database
            App.database.writeToDeliveriesTable(order.getOrderNo(),
                    order.getW3wDeliveryLocation(),
                    order.getTotalCost());

            moveCount += App.database.writeToFlightpathTable(order.getOrderNo(), path);
            totalDeliveredOrderCost += order.getTotalCost();
            //update geoJson temporary list
            for (LongLat node: path)
            {
                assert node.isConfined();
                pointsForLineString.add(Point.fromLngLat(node.getLongitude(), node.getLatitude()));
            }
            return true;
        }
        else
        {
            moveCount += App.database.writeToFlightpathTable("return", pathBackToAppleton);
            return false;
        }
    }

    public int getMoveCount() { return moveCount; }
}
