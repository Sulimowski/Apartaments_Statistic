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
    private static final String BASE_URL = "https://metrohouse.pl/na-sprzedaz/mieszkanie/";
    private static final String PROPERTY_PATH = "/rynek-wtorny/";
    private Set<String> announcementUrls = new HashSet<>();

    // Scrapes apartments for all available cities
    public Map<String, City> scrapeApartments() throws IOException {
        return scrapeApartmentsForCity(null);
    }

    // Scrapes apartments for a specific city or all cities if cityName is null
    public Map<String, City> scrapeApartmentsForCity(String cityName) throws IOException {
        String targetUrl = cityName != null ? constructCityUrl(cityName) : constructCityUrl("warszawa");
        Document mainDoc = Jsoup.connect(targetUrl).get();

        extractAnnouncementUrls(mainDoc);

        return extractCityApartments(cityName);
    }

    // Constructs the URL for the given city
    private String constructCityUrl(String cityName) {
        return BASE_URL + cityName.toLowerCase() + PROPERTY_PATH;
    }

    // Extracts announcement URLs from the main document
    private void extractAnnouncementUrls(Document document) {
        Elements links = document.select("a[href*='/nieruchomosc/']");
        for (Element link : links) {
            announcementUrls.add(link.absUrl("href"));
        }
        System.out.println("Found " + announcementUrls.size() + " announcement URLs.");
    }

    // Extracts apartments and organizes them by city
    private Map<String, City> extractCityApartments(String cityName) throws IOException {
        Map<String, City> cities = new HashMap<>();

        for (String url : announcementUrls) {
            System.out.println("Processing URL: " + url);
            Document document = Jsoup.connect(url).get();
            Apartamets apartment = new Apartamets(
                    extractElementContent(document, "meta[itemprop=productID]"),
                    extractElementContent(document, "meta[itemprop=price]"),
                    extractElementContent(document, "meta[itemprop=name]")
            );

            if (apartment.getID() == null || apartment.getPrice() == null || apartment.getLocation() == null) {
                System.out.println("Skipped incomplete apartment data.");
                continue; // Skip incomplete entries
            }

            String foundCityName = getCityNameFromLocation(apartment.getLocation());
            if (foundCityName != null && (cityName == null || cityName.equalsIgnoreCase(foundCityName))) {
                cities.computeIfAbsent(foundCityName, City::new).AddApart(apartment);
                System.out.println("Added apartment: " + apartment);
            } else {
                System.out.println("Skipped apartment in different city: " + apartment.getLocation());
            }
        }

        return cities;
    }

    // Extracts content from a document element based on the provided CSS query
    private String extractElementContent(Document doc, String cssQuery) {
        Elements elements = doc.select(cssQuery);
        return elements.isEmpty() ? null : elements.attr("content");
    }

    // Extracts city name from the location string
    private String getCityNameFromLocation(String location) {
        if (location != null) {
            String[] parts = location.split("\\s+");
            if (parts.length >= 4) {
                return parts[3]; // Assuming the city name is the fourth word
            }
        }
        return null;
    }

    // Method to get the URL of the next page for pagination
    private String getNextPageUrl(Document document) {
        Element nextPageLink = document.selectFirst("a.next");
        return nextPageLink != null ? nextPageLink.absUrl("href") : null;
    }

    // Method to handle scraping of additional apartments for pagination
    public void scrapeAdditionalApartmentsForCity(String cityName, int requiredCount) throws IOException {
        String targetUrl = constructCityUrl(cityName);
        int totalScraped = 0;

        while (requiredCount > 0) {
            Document mainDoc = Jsoup.connect(targetUrl).get();
            Elements links = mainDoc.select("a[href*='/nieruchomosc/']");

            for (Element link : links) {
                if (requiredCount <= 0) break;

                String url = link.absUrl("href");
                if (!announcementUrls.contains(url)) {
                    announcementUrls.add(url);
                    Document document = Jsoup.connect(url).get();
                    Apartamets apartment = new Apartamets(
                            extractElementContent(document, "meta[itemprop=productID]"),
                            extractElementContent(document, "meta[itemprop=price]"),
                            extractElementContent(document, "meta[itemprop=name]")
                    );

                    if (apartment.getID() == null || apartment.getPrice() == null || apartment.getLocation() == null) {
                        System.out.println("Skipped incomplete apartment data.");
                        continue; // Skip incomplete entries
                    }

                    String foundCityName = getCityNameFromLocation(apartment.getLocation());
                    if (foundCityName != null && cityName.equalsIgnoreCase(foundCityName)) {
                        Main.cities.computeIfAbsent(foundCityName, City::new).AddApart(apartment);
                        System.out.println("Added apartment: " + apartment);
                        requiredCount--;
                        totalScraped++;
                    } else {
                        System.out.println("Skipped apartment in different city: " + apartment.getLocation());
                    }
                }
            }

            targetUrl = getNextPageUrl(mainDoc);
            if (targetUrl == null) break; // No more pages
        }

        System.out.println("Total new apartments scraped: " + totalScraped);
    }
}
