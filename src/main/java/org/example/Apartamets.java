package org.example;

public class Apartamets implements Comparable<Apartamets> {
    private String ID;
    private String price;
    private String location;

    public Apartamets(String ID, String price, String location) {
        this.ID = ID;
        this.price = price;
        this.location = location;
    }

    public String getID() {
        return ID;
    }

    public String getLocation() {
        return location;
    }

    public String getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "ID: " + ID + "\nLocation: " + location + "\nPrice: " + price;
    }

    @Override
    public int compareTo(Apartamets o) {
        return this.location.compareTo(o.getLocation());
    }
}