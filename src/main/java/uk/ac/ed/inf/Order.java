package uk.ac.ed.inf;

import java.util.ArrayList;

/**
 * Class to hold all information relating the orders that have been placed into the database prior to execution.
 */
public class Order
{
    private final String orderNo;
    private final String w3wDeliveryLocation;
    private final LongLat deliveryLocation;
    private ArrayList<LongLat> pickupLocations;
    private ArrayList<String> items;
    private int totalCost;
    private final static int DELIVERY_FEE = 50;
    public static int totalPlacedOrderCost;
    private final static int ITEM_NOT_FOUND_FLAG_VAL = -1;

    /**
     * Class constructor. Assigns the order number, and calls LocationConversion to convert the what3words address of
     * the delivery location to longitude and latitude co-ordinates using words.json.
     * @param orderNo the order number
     * @param w3w the what3words delivery location
     */
    public Order(String orderNo, String w3w)
    {
        this.orderNo = orderNo;
        this.w3wDeliveryLocation = w3w;
        deliveryLocation = LocationConversion.w3wToLongLat(w3w);
    }

    /**
     * Method used to assign the information contained within the orderDetails database table to each order. This includes
     * the items being delivered, the locations of the businesses that need to be visited to pick up the order, and the
     * total cost of the order.
     * @param items the items that have been ordered under this order number
     * @param menus the menus.json file, parsed into respective java objects
     */
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
                if (pence != ITEM_NOT_FOUND_FLAG_VAL)
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

        setTotalCost(cost);
        setPickupLocations(LocationConversion.wordsToLongLats(locations));
    }

    /**
     * Assigns the pickup locations to this Order object.
     * @param pickupLocations the pickup locations
     */
    private void setPickupLocations(ArrayList<LongLat> pickupLocations) { this.pickupLocations = pickupLocations; }

    /**
     * Assigns the total order cost in pence.
     * @param cost the total cost
     */
    private void setTotalCost(int cost) { totalCost = cost; }

    /**
     * Retrieves the total cost in pence.
     * @return the total cost
     */
    public int getTotalCost() { return totalCost; }

    /**
     * Retrieves the order number.
     * @return the order number
     */
    public String getOrderNo() { return orderNo; }

    /**
     * Retrieves the delivery location as a what3words string.
     * @return the what3words address of the delivery location
     */
    public String getW3wDeliveryLocation() { return w3wDeliveryLocation; }

    /**
     * Retrieves the delivery location in longitude and latitude format.
     * @return the delivery location as a LongLat object
     */
    public LongLat getDeliveryLocation() { return deliveryLocation; }

    /**
     * Retrieves the pickup locations in longitude and latitude format.
     * @return the pickup locations as a list of LongLat objects
     */
    public ArrayList<LongLat> getPickupLocations() { return pickupLocations; }

    /**
     * Retrieves the list of items that are to be delivered.
     * @return the items
     *
     */
    public ArrayList<String> getItems() { return items; }
}
