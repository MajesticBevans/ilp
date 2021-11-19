package uk.ac.ed.inf;

import java.util.ArrayList;

public class Order
{
    private final String orderNo;
    private final String w3wDeliveryLocation;
    private final LongLat deliveryLocation;
    private ArrayList<LongLat> pickupLocations;
    private ArrayList<String> items;
    private int totalDeliveryCost;
    private final int DELIVERY_FEE = 50;

    public Order(String orderNo, String w3w)
    {
        this.orderNo = orderNo;
        this.w3wDeliveryLocation = w3w;
        deliveryLocation = LocationConversion.w3wToLongLat(App.webServer, w3w);
    }

    public void setOrderDetails(ArrayList<String> items, ArrayList<Shop> menus)
    {
        this.items = items;
        int cost = DELIVERY_FEE;
        ArrayList<String> locations = new ArrayList<>();

        for (Shop shop : menus)
        {
            for (String item : items)
            {
                int pence = shop.getPence(item);
                if (pence != -1)
                {
                    cost += pence;

                    String location = shop.getLocation();
                    if (!locations.contains(location))
                    {
                        locations.add(location);
                    }
                }
            }
        }

        setTotalDeliveryCost(cost);
        setPickupLocations(LocationConversion.wordsToLongLats(App.webServer, locations));
    }

    private void setPickupLocations(ArrayList<LongLat> pickupLocations) { this.pickupLocations = pickupLocations; }

    private void setTotalDeliveryCost(int cost) { this.totalDeliveryCost = cost; }

    public String getOrderNo() { return orderNo; }

    public String getW3wDeliveryLocation() { return w3wDeliveryLocation; }

    public LongLat getDeliveryLocation() { return deliveryLocation; }

    public ArrayList<LongLat> getPickupLocations() { return pickupLocations; }

    public ArrayList<String> getItems() { return items; }
}
