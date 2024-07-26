package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;


public class Main {
    protected static Map<String, City> cities = new HashMap<>();
    private static City selectedCity;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Apartment Scraper");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);

            JButton scrapeButton = new JButton("Scrape Apartments");
            JButton avgPriceButton = new JButton("Average Price");
            JButton showApartmentsButton = new JButton("Show Apartments");
            JButton selectCityButton = new JButton("Select City");
            JTextArea resultArea = new JTextArea();
            resultArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(resultArea);

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            progressBar.setVisible(false);

            JLabel selectedCityLabel = new JLabel("Selected City: None");

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(scrapeButton);
            buttonPanel.add(avgPriceButton);
            buttonPanel.add(showApartmentsButton);
            buttonPanel.add(selectCityButton);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(buttonPanel, BorderLayout.NORTH);
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(progressBar, BorderLayout.SOUTH);
            mainPanel.add(selectedCityLabel, BorderLayout.EAST);

            frame.getContentPane().add(mainPanel);
            frame.setVisible(true);

            scrapeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    progressBar.setVisible(true);
                    resultArea.setText("");

                    SwingWorker<Map<String, City>, Void> worker = new SwingWorker<Map<String, City>, Void>() {
                        @Override
                        protected Map<String, City> doInBackground() throws Exception {
                            Scraper scraper = new Scraper();
                            return scraper.scrapeApartments();
                        }

                        @Override
                        protected void done() {
                            try {
                                cities = get();
                                selectedCity = null; // Reset selected city after scraping
                                selectedCityLabel.setText("Selected City: None");
                                resultArea.setText("Scraping completed. Cities available:\n" + String.join("\n", cities.keySet()));
                            } catch (Exception ex) {
                                resultArea.setText("Failed to scrape apartments, please try one more time.");
                            } finally {
                                progressBar.setVisible(false);
                            }
                        }
                    };

                    worker.execute();
                }
            });

            avgPriceButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selectedCity != null) {
                        double avgPrice = calculateAveragePrice(selectedCity.getCityApart().values());
                        resultArea.setText("Average Price in " + selectedCity.getCity_name() + ": " + String.format("%.2f PLN", avgPrice));
                    } else {
                        resultArea.setText("No city selected. Please select a city first.");
                    }
                }
            });

            showApartmentsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selectedCity != null) {
                        String input = JOptionPane.showInputDialog(frame, "Enter the number of apartments to show:", "Show Apartments", JOptionPane.QUESTION_MESSAGE);
                        try {
                            int count = Integer.parseInt(input);
                            if (count < 1) {
                                throw new NumberFormatException();
                            }

                            if (count > selectedCity.getCityApart().size()) {
                                // Scrape additional apartments for the city
                                progressBar.setVisible(true);
                                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                                    @Override
                                    protected Void doInBackground() throws Exception {
                                        int requiredCount = count - selectedCity.getCityApart().size();
                                        scrapeAdditionalApartmentsForCity(selectedCity.getCity_name(), requiredCount);
                                        return null;
                                    }

                                    @Override
                                    protected void done() {
                                        SwingUtilities.invokeLater(() -> {
                                            try {
                                                // Display apartments
                                                StringBuilder result = new StringBuilder();
                                                int i = 0;
                                                for (Apartamets apartamets : selectedCity.getCityApart().values()) {
                                                    if (i >= count) break;
                                                    result.append(apartamets).append("\n\n");
                                                    i++;
                                                }
                                                resultArea.setText(result.toString());
                                            } catch (Exception ex) {
                                                resultArea.setText("Failed to load additional apartments.");
                                            } finally {
                                                progressBar.setVisible(false);
                                            }
                                        });
                                    }
                                };

                                worker.execute();
                            } else {
                                // Display apartments
                                StringBuilder result = new StringBuilder();
                                int i = 0;
                                for (Apartamets apartamets : selectedCity.getCityApart().values()) {
                                    if (i >= count) break;
                                    result.append(apartamets).append("\n\n");
                                    i++;
                                }
                                resultArea.setText(result.toString());
                            }
                        } catch (NumberFormatException ex) {
                            resultArea.setText("Invalid number. Please enter a positive integer.");
                        }
                    } else {
                        resultArea.setText("No city selected. Please select a city first.");
                    }
                }

                private void scrapeAdditionalApartmentsForCity(String cityName, int requiredCount) throws IOException {
                    Scraper scraper = new Scraper();
                    scraper.scrapeAdditionalApartmentsForCity(cityName, requiredCount);
                }
            });

            selectCityButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (cities.isEmpty()) {
                        resultArea.setText("No cities available. Please scrape first.");
                        return;
                    }

                    String[] cityNames = cities.keySet().toArray(new String[0]);
                    String selectedCityName = (String) JOptionPane.showInputDialog(frame, "Select a city:", "Select City", JOptionPane.QUESTION_MESSAGE, null, cityNames, cityNames[0]);

                    if (selectedCityName != null) {
                        selectedCity = cities.get(selectedCityName);
                        selectedCityLabel.setText("Selected City: " + selectedCity.getCity_name());
                        resultArea.setText("Selected city: " + selectedCity.getCity_name());
                    }
                }
            });
        });
    }

    private static double calculateAveragePrice(Collection<Apartamets> apartments) {
        if (apartments == null || apartments.isEmpty()) {
            return 0;
        }

        double totalPrice = 0;
        int count = 0;

        for (Apartamets apartamets : apartments) {
            try {
                double price = Double.parseDouble(apartamets.getPrice().replaceAll("[^\\d.]", ""));
                totalPrice += price;
                count++;
            } catch (NumberFormatException e) {
                // Handle invalid price format if needed
            }
        }

        return count > 0 ? totalPrice / count : 0;
    }
}
