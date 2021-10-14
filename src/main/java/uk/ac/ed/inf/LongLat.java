package uk.ac.ed.inf;

/**
 * Class for representing a point using longitude and latitude values.
 */
public class LongLat
{
    final double MINIMUM_LONGITUDE = -3.192473;
    final double MAXIMUM_LONGITUDE = -3.184319;
    final double MINIMUM_LATITUDE = 55.942617;
    final double MAXIMUM_LATITUDE = 55.946233;
    final double CLOSE_DISTANCE = 0.00015;

    public double longitude, latitude;
    private final boolean confined;

    /**
     * Class constructor. Stores the coordinates and determines whether the point is within the drone confinement area.
     * @param longitude double - input longitude
     * @param latitude double - input latitude
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
     * Returns whether or not these coordinates are within the drone confinement area.
     * @return a boolean value
     */
    public boolean isConfined() { return confined; }

    /**
     * Calculates the Pythagorean distance from this point to a second point.
     * @param secondPoint the second point
     * @return the distance as a double
     */
    public double distanceTo(LongLat secondPoint)
    {
        double longitudeDistance = this.longitude - secondPoint.longitude;
        double latitudeDistance = this.latitude - secondPoint.latitude;

        return Math.pow(longitudeDistance, 2) + Math.pow(latitudeDistance, 2);
    }

    /**
     * Returns whether the distance from this point to a second point is small enough to be 'close'.
     * @param secondPoint the second point
     * @return a boolean value
     */
    public boolean closeTo(LongLat secondPoint)
    {
        return distanceTo(secondPoint) < CLOSE_DISTANCE;
    }

    /**
     * Calculates the next position of this point, were it to move, according to the spec, along the angle specified by
     * the parameter 'angle'.
     * @param angle the specified angle
     * @return a LongLat object with coordinates of the next position
     */
    public LongLat nextPosition(int angle)
    {
        if (angle % 10 != 0 || angle < 0 || angle > 350)
        {
            throw new IllegalArgumentException("Angle must be a multiple of 10 and between 0 and 350, inclusive.");
        }

        double nextLongitude;
        double nextLatitude;

        if (angle < 90)
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
            nextLatitude = this.latitude = Math.sin(Math.toRadians(angle - 180)) * CLOSE_DISTANCE;
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
}
