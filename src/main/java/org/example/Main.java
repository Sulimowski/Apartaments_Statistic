package org.example;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    private static Map<String, City> cities = new HashMap<>();
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

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(scrapeButton);
            buttonPanel.add(avgPriceButton);
            buttonPanel.add(showApartmentsButton);
            buttonPanel.add(selectCityButton);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(buttonPanel, BorderLayout.NORTH);
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(progressBar, BorderLayout.SOUTH);

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
                                resultArea.setText("Scraping completed. Cities available:\n" + String.join("\n", cities.keySet()));
                            } catch (Exception ex) {
                                resultArea.setText("Failed to scrape apartments.");
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
                            count = Math.min(count, selectedCity.getCityApart().size());

                            StringBuilder result = new StringBuilder();
                            int i = 0;
                            for (Apartamets apartamets : selectedCity.getCityApart().values()) {
                                if (i >= count) break;
                                result.append(apartamets).append("\n\n");
                                i++;
                            }
                            resultArea.setText(result.toString());
                        } catch (NumberFormatException ex) {
                            resultArea.setText("Invalid number. Please enter a positive integer.");
                        }
                    } else {
                        resultArea.setText("No city selected. Please select a city first.");
                    }
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
