package uk.ac.ed.inf;

public class W3WDetails
{
    private String country, nearestPlace, words, language, map;
    public LngLat coordinates;
    private Square square;

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
    public class LngLat
    {
        public double lng;
        public double lat;

        public LngLat(double lng, double lat)
        {
            this.lng = lng;
            this.lat = lat;
        }
        public double getLng() { return lng; }
        public double getLat() { return lat; }
    }

    private class Square
    {
        private LngLat southwest, northeast;
        public Square(LngLat southwest, LngLat northeast)
        {
            this.southwest = southwest;
            this.northeast = northeast;
        }
    }
}
