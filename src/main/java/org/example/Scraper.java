package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Scraper {
    private String mainUrl = "https://metrohouse.pl/na-sprzedaz/mieszkanie/warszawa/rynek-wtorny/";
    private Document mainDoc = null;
    private Set<String> announcementUrls = new HashSet<>();

    public Map<String, City> scrapeApartments() throws IOException {
        Map<String, City> cities = new HashMap<>();
        mainDoc = Jsoup.connect(mainUrl).get();
        Elements links = mainDoc.select("a[href*='/nieruchomosc/']");

        for (Element link : links) {
            announcementUrls.add(link.absUrl("href"));
        }

        for (String url : announcementUrls) {
            Document document = Jsoup.connect(url).get();
            Apartamets apartamets = new Apartamets(
                    ApartId(document),
                    ApartPrice(document),
                    ApartName(document)
            );

            String cityName = getCityNameFromLocation(apartamets.getLocation());
            if (cityName != null) {
                City city = cities.computeIfAbsent(cityName, City::new);
                city.AddApart(apartamets);
            }
        }

        return cities;
    }

    private String ApartId(Document doc) {
        Elements productID = doc.select("meta[itemprop=productID]");
        if (!productID.isEmpty()) {
            return productID.attr("content");
        }
        return null;
    }

    private String ApartName(Document doc) {
        Elements name = doc.select("meta[itemprop=name]");
        if (!name.isEmpty()) {
            return name.attr("content");
        }
        return null;
    }

    private String ApartPrice(Document doc) {
        Elements price = doc.select("meta[itemprop=price]");
        if (!price.isEmpty()) {
            return price.attr("content");
        }
        return null;
    }

    private String getCityNameFromLocation(String location) {
        if (location != null && location.split("\\s+").length >= 4) {
            String[] parts = location.split("\\s+");
            return parts[3]; // Assuming the city name is the fourth word
        }
        return null;
    }
}
