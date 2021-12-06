package uk.ac.ed.inf;

/**
 * Class for representing a point using longitude and latitude values.
 */
public class LongLat
{
    final static double MINIMUM_LONGITUDE = -3.192473;
    final static double MAXIMUM_LONGITUDE = -3.184319;
    final static double MINIMUM_LATITUDE = 55.942617;
    final static double MAXIMUM_LATITUDE = 55.946233;
    final static double CLOSE_DISTANCE = 0.00015;
    final static int HOVER_VALUE = -999;

    private final double longitude;
    private final double latitude;
    private final boolean confined;

    /**
     * Class constructor. Stores the coordinates and determines whether the point is within the drone confinement area.
     * @param longitude input longitude
     * @param latitude input latitude
     */
    public LongLat(double longitude, double latitude)
    {
        this.longitude = longitude;
        this.latitude = latitude;

        confined = this.longitude < MAXIMUM_LONGITUDE &&
                   this.longitude > MINIMUM_LONGITUDE &&
                   this.latitude < MAXIMUM_LATITUDE &&
                   this.latitude > MINIMUM_LATITUDE;
    }

    /**
     * Returns whether these coordinates are within the drone confinement area.
     * @return a boolean value
     */
    public boolean isConfined() { return confined; }

    /**
     * Calculates the Pythagorean distance from this point to a second point.
     * @param secondPoint the second point
     * @return the distance
     */
    public double distanceTo(LongLat secondPoint)
    {
        double longitudeDistance = this.longitude - secondPoint.longitude;
        double latitudeDistance = this.latitude - secondPoint.latitude;

        return Math.sqrt(longitudeDistance * longitudeDistance + latitudeDistance * latitudeDistance);
    }

    /**
     * Returns whether the distance from this point to a second point is small enough to be 'close'.
     * @param secondPoint the second point
     * @return true if close, false otherwise
     */
    public boolean closeTo(LongLat secondPoint)
    {
        return distanceTo(secondPoint) < CLOSE_DISTANCE;
    }

    /**
     * Method to get the angle between this object and a second LongLat object.
     * @param secondPoint the second LongLat
     * @return the angle from this LongLat to the secondPoint
     */
    public int angleTo(LongLat secondPoint)
    {
        double secondLongitude = secondPoint.getLongitude();
        double secondLatitude = secondPoint.getLatitude();

        double tanLatOverLong = Math.toDegrees(Math.atan(
                Math.abs(secondLatitude - latitude) / Math.abs(secondLongitude - longitude)));

        double tanLongOverLat = Math.toDegrees(Math.atan(
                Math.abs(secondLongitude - longitude) / Math.abs(secondLatitude - latitude)));

        if (secondLongitude == longitude && secondLatitude == latitude) { return HOVER_VALUE; }
        else if (secondLongitude > longitude && secondLatitude == latitude) { return 0; }
        else if (secondLongitude > longitude && secondLatitude > latitude)
        {
            return (int)Math.round(tanLatOverLong/10) * 10;
        }
        else if (secondLongitude == longitude && secondLatitude > latitude) {
            return 90;
        }
        else if (secondLongitude < longitude && secondLatitude > latitude)
        {
            double rawAngle = 90 + tanLongOverLat;

            return (int)Math.round(rawAngle/10) * 10;
        }
        else if (secondLongitude < longitude && secondLatitude == latitude) { return 180; }
        else if (secondLongitude < longitude && secondLatitude < latitude)
        {
            double rawAngle = 180 + tanLatOverLong;
            return (int)Math.round(rawAngle/10) * 10;
        }
        else if (secondLongitude == longitude) { return 270; }
        else
        {
            double rawAngle = 270 + tanLongOverLat;
            return (int)Math.round(rawAngle/10) * 10;
        }
    }

    /**
     * Calculates the next position of the drone were it to move according to the spec, from the current coordinates,
     * along the angle specified by the parameter 'angle'. If given the angle -999, the drone will hover,
     * and the position will not change. Otherwise, the angle must be a positive multiple of 10 between 0 and 350
     * inclusive.
     * @param angle the specified angle
     * @return a LongLat object with coordinates of the next position
     */
    public LongLat nextPosition(int angle)
    {
        //checks hover value
        if (angle == HOVER_VALUE) { return this; }

        //checks angle is valid
        assert (angle >= 0);
        assert (angle <= 350);
        assert (angle % 10 == 0);

        double nextLongitude;
        double nextLatitude;

        if (angle == 0)
        {
            nextLongitude = this.longitude + CLOSE_DISTANCE;
            nextLatitude = this.latitude;
        }
        else if (angle < 90)
        {
            nextLongitude = this.longitude + Math.cos(Math.toRadians(angle)) * CLOSE_DISTANCE;
            nextLatitude = this.latitude + Math.sin(Math.toRadians(angle)) * CLOSE_DISTANCE;
        }
        else if (angle == 90)
        {
            nextLongitude = this.longitude;
            nextLatitude = this.latitude + CLOSE_DISTANCE;
        }
        else if (angle < 180)
        {
            nextLongitude = this.longitude - Math.sin(Math.toRadians(angle - 90)) * CLOSE_DISTANCE;
            nextLatitude = this.latitude + Math.cos(Math.toRadians(angle - 90)) * CLOSE_DISTANCE;
        }
        else if (angle == 180)
        {
            nextLongitude = this.longitude - CLOSE_DISTANCE;
            nextLatitude = this.latitude;
        }
        else if (angle < 270)
        {
            nextLongitude = this.longitude - Math.cos(Math.toRadians(angle - 180)) * CLOSE_DISTANCE;
            nextLatitude = this.latitude - Math.sin(Math.toRadians(angle - 180)) * CLOSE_DISTANCE;
        }
        else if (angle == 270)
        {
            nextLongitude = this.longitude;
            nextLatitude = this.latitude - CLOSE_DISTANCE;
        }
        else
        {
            nextLongitude = this.longitude + Math.sin(Math.toRadians(angle - 270)) * CLOSE_DISTANCE;
            nextLatitude = this.latitude - Math.cos(Math.toRadians(angle - 270)) * CLOSE_DISTANCE;
        }
        return new LongLat(nextLongitude, nextLatitude);
    }

    /**
     * Retrieves the longitude value of this LongLat.
     * @return the longitude
     */
    public double getLongitude() { return longitude; }

    /**
     * Retrieves the latitude value of this LongLat.
     * @return the latitude
     */
    public double getLatitude() { return latitude; }
}
