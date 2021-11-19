package uk.ac.ed.inf;
import java.util.ArrayList;

/**
 * Class to allow the menus.json data to be parsed into corresponding java objects.
 */
public class Shop
{
    /**
     * Class to represent the individual items on the shop menus.
     */
    private static class MenuItem
    {
        private final String item;
        private final int pence;

        /**
         * Class constructor. Simply assigns the values of the two attributes.
         * @param item the name of the item on the menu
         * @param pence the price of the item in pence
         */
        public MenuItem(String item, int pence)
        {
            this.item = item;
            this.pence = pence;
        }
    }

    private final String name;
    private final String location;
    private final ArrayList<MenuItem> menu;

    /**
     * Class constructor. Simply assigns the values of the three attributes.
     * @param name the name of the shop
     * @param location the what3words location of the shop
     * @param menu the contents of the shop's menu, including name and price via the MenuItem class
     */
    public Shop(String name, String location, ArrayList<MenuItem> menu)
    {
        this.name = name;
        this.location = location;
        this.menu = menu;
    }

    /**
     * Returns the price of an item in pence, given its name.
     * @param itemName the name of the item
     * @return the price of the item in pence, if it is found on the shop menu. -1 otherwise
     */
    public int getPence(String itemName)
    {
        for (MenuItem menuItem: menu)
        {
            if (itemName.equals(menuItem.item))
            {
                return menuItem.pence;
            }
        }
        return -1;
    }

    public String getLocation() { return location; }
}
