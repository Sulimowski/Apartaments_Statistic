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
    private static final String PROPERTY_PATH = "/-/rynek-wtorny/";
    private Set<String> announcementUrls = new HashSet<>();

    // Scrapes apartments for all available cities
    public Map<String, City> scrapeApartments() throws IOException {
        return scrapeApartmentsForCity(null);
    }

    // Scrapes apartments for all cities
    public Map<String, City> scrapeApartmentsForCity(String cityName) throws IOException {
        String targetUrl =  BASE_URL + PROPERTY_PATH;
        Document mainDoc = Jsoup.connect(targetUrl).get();

        extractAnnouncementUrls(mainDoc);

        return extractCityApartments(cityName);
    }

    // Constructs the URL for the given city
    private String constructCityUrl(String cityName) {
        String clearedCityname = cityName.toLowerCase().replace(",","") .replace("ó", "o").replace("Ł","l").replace("ź","z").replace("ą","a").replace("ć","c");
        return BASE_URL + clearedCityname + PROPERTY_PATH;
    }

    // Extracts announcement URLs from the main document
    private void extractAnnouncementUrls(Document document) {
        announcementUrls.clear();
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
        if (location != null  && !location.trim().isEmpty()) {
            String[] parts = location.split("\\s+");
            if (parts.length >= 4) {
                return parts[3]; //the city name is the fourth word
            }
        }
        return null;
    }

    // Method to get the URL of the next page for pagination
    private String getNextPageUrl(String currentUrl, int currentPage) {
        return currentUrl + "/page-" + currentPage;
    }

    // Method to handle scraping of additional apartments for pagination
    public void scrapeAdditionalApartmentsForCity(String cityName, int requiredCount) throws IOException {
        String targetUrl = constructCityUrl(cityName);
        System.out.println("Starting URL: " + targetUrl);
        int totalScraped = 0;
        int currentPage = 1;

        while (requiredCount > 0) {
            System.out.println("Fetching page: " + targetUrl);
            Document mainDoc = Jsoup.connect(targetUrl).get();
            Elements links = mainDoc.select("a[href*='/nieruchomosc/']");

            System.out.println("Found " + links.size() + " links on the page.");

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
                    if (cityName.equalsIgnoreCase(foundCityName)) {
                        City city = Main.cities.computeIfAbsent(foundCityName, City::new);
                        if (!city.getCityApart().containsKey(apartment.getID())) {
                            city.AddApart(apartment);
                            System.out.println("Added apartment: " + apartment);
                            requiredCount--;
                            totalScraped++;
                        } else {
                            System.out.println("Skipped duplicate apartment: " + apartment.getID());
                        }
                    } else {
                        System.out.println("Skipped apartment in different city: " + apartment.getLocation());
                    }
                } else {
                    System.out.println("Skipped already processed URL: " + url);
                }
            }

            currentPage++;
            targetUrl = getNextPageUrl(targetUrl, currentPage);

            // If we run out of pages, stop scraping
            if (targetUrl == null) {
                System.out.println("No more pages to fetch.");
                break;
            }
        }

        System.out.println("Total new apartments scraped: " + totalScraped);
    }
}
