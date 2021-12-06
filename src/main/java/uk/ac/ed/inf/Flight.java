package uk.ac.ed.inf;

import com.mapbox.geojson.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that contains all information relating to the flight of the drone, including the pathing algorithm, its helper
 * functions, and methods to commit this path to the database.
 */
public class Flight
{
    private final ArrayList<Order> orders;
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
     * @param noFlyZones the no-fly-zones from /buildings/no-fly-zones.geojson
     */
    public Flight(ArrayList<Order> orders, ArrayList<Polygon> noFlyZones)
    {
        this.orders = orders;
        this.noFlyZones = noFlyZones;
    }

    /**
     * Top-level function that decides the order in which locations are visited during the flightpath, calling the
     * appropriate methods to generate the sub-paths between them, and to commit these paths to the database.
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
                if (orderPath.size() > 0) { previousLocation = orderPath.get(orderPath.size() - 1); }
                else { previousLocation = pickup; }
            }
            orderPath.add(previousLocation); // adds pickup location again to represent the hover move in path
            LongLat deliveryLocation = order.getDeliveryLocation();

            orderPath.addAll(createSubPath(previousLocation, deliveryLocation));
            previousLocation = orderPath.get(orderPath.size() - 1);
            orderPath.add(previousLocation); // adds delivery location again to represent hover move in path

            if (!commitDeliveryPath(order, orderPath))
            {
                return FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(pointsForLineString))).toJson();
            }
        }

        //return to appleton
        ArrayList<LongLat>pathBackToAppleton = createSubPath(previousLocation, APPLETON_TOWER);

        moveCount += App.database.writeToFlightpathTable("return", pathBackToAppleton);
        for (LongLat node: pathBackToAppleton)
        {
            assert node.isConfined();
            pointsForLineString.add(Point.fromLngLat(node.getLongitude(), node.getLatitude()));
        }

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
            LongLat next = previous.nextPosition(previous.angleTo(destination));
            if (lineEntersNoFlyZone(previous, next))
            {
                return new ArrayList<>();
            }
            previous = next;
            assert previous.isConfined();

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
            ArrayList<LongLat> destinationTestPath = straightLineFromTo(node, destination);
            ArrayList<LongLat> turnTestPath = testPath(node, destination, turnAngle);
            if (destinationTestPath.size() > 0)
            {
                returnPath.addAll(destinationTestPath);
                return returnPath;
            }
            else if (turnTestPath.size() > 0)
            {
                turnAngle = (turnAngle + 10 * flag) % 360;
                ArrayList<LongLat> maxTurnPath = testPath(node, destination, turnAngle);

                if (flag == GREATER_FLAG)
                {
                    while (maxTurnPath.size() > 0 && turnAngle > node.angleTo(destination))
                    {
                        turnTestPath = maxTurnPath;
                        turnAngle = (turnAngle + 10 * flag) % 360;
                        maxTurnPath = testPath(node, destination, turnAngle);
                    }
                }
                else
                {
                    while (maxTurnPath.size() > 0 && turnAngle < node.angleTo(destination))
                    {
                        turnTestPath = maxTurnPath;
                        turnAngle = (turnAngle + 10 * flag) % 360;
                        maxTurnPath = testPath(node, destination, turnAngle);
                    }
                }

                returnPath.addAll(turnTowardsDestination(turnTestPath, destination, turnAngle, flag));
                return returnPath;
            }
            else { returnPath.add(node); }
        }
        System.err.println("Algorithm failed");
        assert false;
        return returnPath;
    }

    /**
     * Method that creates a completely straight line path at a given angle from the given origin, and providing no
     * no-fly-zones are breached, continues this path until either a point enters close to the destination, or the edge
     * of the confinement zone is reached.
     * @param origin the origin point
     * @param destination the planned final destination
     * @param angle the angle the path will follow
     * @return the path as described in the method description, or an empty list if a no-fly-zone is breached
     */
    private ArrayList<LongLat> testPath(LongLat origin, LongLat destination, int angle)
    {
        ArrayList<LongLat> path = new ArrayList<>();
        path.add(origin);
        LongLat next = origin.nextPosition(angle);

        while (next.isConfined() && !next.closeTo(destination))
        {
            LongLat curr = path.get(path.size() - 1);
            if (lineEntersNoFlyZone(curr, next)) { return new ArrayList<>(); }
            path.add(next);
            next = next.nextPosition(angle);
        }
        if (next.closeTo(destination)) { path.add(next); }
        return path;
    }

    /**
     * Method that returns whether the line between two points breaches any of the no-fly-zones.
     * @param point1 the first point
     * @param point2 the second point
     * @return true if no-fly-zone is breached, false otherwise
     */
    private boolean lineEntersNoFlyZone(LongLat point1, LongLat point2)
    {
        for (Polygon poly : noFlyZones)
        {
            List<Point> co_ords = poly.coordinates().get(0);
            for (int i = 0; i < co_ords.size() - 1; i++)
            {
                LongLat curr = LocationConversion.pointToLongLat(co_ords.get(i));
                LongLat next = LocationConversion.pointToLongLat(co_ords.get(i + 1));
                if (linesIntersect(point1, point2, curr, next))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method that returns whether the lines between two pairs of points intersect.
     * @param line1Point1 the first point of the first pair
     * @param line1Point2 the second point of the first pair
     * @param line2Point1 the first point of the second pair
     * @param line2Point2 the second point of the second pair
     * @return true if the lines intersect, false otherwise
     */
    private boolean linesIntersect(LongLat line1Point1, LongLat line1Point2, LongLat line2Point1, LongLat line2Point2)
    {
        // for this function I will treat coordinates as if longitude is the x value, and latitude is the y value

        if (line1Point1 == line1Point2 || line2Point1 == line2Point2) { return false; }

        // if these lines do not exist in the same x interval, they cannot intersect
        if (Math.max(line1Point1.getLongitude(), line1Point2.getLongitude()) <
                Math.min(line2Point1.getLongitude(), line2Point2.getLongitude())) { return false; }

        // if the lines are vertical, when calculating the line gradient there will be a division by 0. This if statement
        // anticipates this and handles the cases where either line is vertical
        if (line1Point1.getLongitude() == line1Point2.getLongitude() && line2Point1.getLongitude() == line2Point2.getLongitude())
        {
            return false;
        }
        else if (line1Point1.getLongitude() == line1Point2.getLongitude())
        {
            if (line2Point1.getLongitude() > line1Point1.getLongitude() != line2Point2.getLongitude() > line1Point1.getLongitude())
            {
                return true;
            }
        }
        else if (line2Point1.getLongitude() == line2Point2.getLongitude())
        {
            if (line1Point1.getLongitude() > line2Point1.getLongitude() != line1Point2.getLongitude() > line2Point1.getLongitude())
            {
                return true;
            }
        }

        //calculate line gradients
        double m1 = (line1Point1.getLatitude() - line1Point2.getLatitude()) /
                (line1Point1.getLongitude() - line1Point2.getLongitude());

        double m2 = (line2Point1.getLatitude() - line2Point2.getLatitude()) /
                (line2Point1.getLongitude() - line2Point2.getLongitude());

        if (m1 == m2) { return false; } // if the lines have the same gradient, they are parallel and so cannot intersect

        //get y intercepts
        double c1 = line1Point1.getLatitude() - m1 * line1Point1.getLongitude();
        double c2 = line2Point1.getLatitude() - m2 * line2Point1.getLongitude();

        //get the x value of the line intersection
        double intersectionXVal = (c2 - c1) / (m1 - m2);

        //if the intersection value calculated is within the overlapping x values, the lines must intersect
        if (intersectionXVal >
                Math.max(Math.min(line1Point1.getLongitude(), line1Point2.getLongitude()), Math.min(line2Point1.getLongitude(), line2Point2.getLongitude())) &&
                (intersectionXVal <
                        Math.min(Math.max(line1Point1.getLongitude(), line1Point2.getLongitude()), Math.max(line2Point1.getLongitude(), line2Point2.getLongitude()))))
        {
            return true;
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
        boolean canGetBackToAppleton = pathBackToAppleton.size() < MAX_MOVE_COUNT - newMoveCount;

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
            for (LongLat node: pathBackToAppleton)
            {
                assert node.isConfined();
                pointsForLineString.add(Point.fromLngLat(node.getLongitude(), node.getLatitude()));
            }
            return false;
        }
    }

    /**
     * Method to retrieve the move count.
     * @return the move count
     */
    public int getMoveCount() { return moveCount; }
}
