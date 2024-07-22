package org.example;
import java.util.HashMap;
import java.util.Map;

public class City {
    private Map<String, Apartamets> cityApartments = new HashMap<>();
    private String cityName;

    public City(String cityName) {
        this.cityName = cityName;
    }

    public void AddApart(Apartamets apartamets) {
        if (apartamets != null) {
            cityApartments.put(apartamets.getID(), apartamets);
        }
    }

    public Map<String, Apartamets> getCityApart() {
        return cityApartments;
    }

    public String getCity_name() {
        return cityName;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("City: ").append(cityName).append("\n");
        result.append("Apartments:\n");
        for (Apartamets apartamets : cityApartments.values()) {
            result.append(apartamets).append("\n\n");
        }
        return result.toString();
    }
}
