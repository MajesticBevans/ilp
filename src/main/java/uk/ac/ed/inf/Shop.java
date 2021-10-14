package uk.ac.ed.inf;
import java.util.ArrayList;
import java.util.List;

public class Shop
{
    private static class MenuItem
    {
        private final String item;
        private final int pence;

        public MenuItem(String item, int pence)
        {
            this.item = item;
            this.pence = pence;
        }
    }

    private final String name;
    private final String location;
    private final ArrayList<MenuItem> menu;

    public Shop(String name, String location, ArrayList<MenuItem> menu)
    {
        this.name = name;
        this.location = location;
        this.menu = menu;
    }

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
}
