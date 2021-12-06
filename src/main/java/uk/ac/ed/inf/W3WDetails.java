package uk.ac.ed.inf;

/**
 * Class to parse the details.json file of the what3words locations.
 */
public class W3WDetails
{
    private final String country;
    private final String nearestPlace;
    private final String words;
    private final String language;
    private final String map;
    public LngLat coordinates;
    private final Square square;

    /**
     * Class constructor. Simply assigns the attributes of the class.
     * @param country the country that contains this location
     * @param nearestPlace the nearest place to this location
     * @param words the 3 words that represent the location
     * @param language the language
     * @param map the map
     * @param coordinates the co-ordinates of the centre of the square
     * @param square the square covered by the words
     */
    public W3WDetails(String country, String nearestPlace, String words, String language, String map, LngLat coordinates, Square square)
    {
        this.country = country;
        this.nearestPlace = nearestPlace;
        this.words = words;
        this.language = language;
        this.map = map;
        this.coordinates = coordinates;
        this.square = square;
    }

    /**
     * Helper class to parse the LngLat json object.
     */
    public class LngLat
    {
        public double lng;
        public double lat;

        /**
         * Class constructor. Simply assigns the longitude and latitude values.
         * @param lng the longitude
         * @param lat the latitude
         */
        public LngLat(double lng, double lat)
        {
            this.lng = lng;
            this.lat = lat;
        }

        /**
         * Retrieves the longitude
         * @return the longitude
         */
        public double getLng() { return lng; }

        /**
         * Retrieves the latitude.
         * @return the latitude
         */
        public double getLat() { return lat; }
    }

    /**
     * Helper class to parse the square json object.
     */
    private class Square
    {
        private final LngLat southwest;
        private final LngLat northeast;

        /**
         * Class constructor. Simply assigns the southwest and northeast co-ordinates as LngLat objects.
         * @param southwest the southwest co-ordinate
         * @param northeast the northeast co-ordinate
         */
        public Square(LngLat southwest, LngLat northeast)
        {
            this.southwest = southwest;
            this.northeast = northeast;
        }
    }
}
