import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RSSReader {
    
    private static final String DATA_FILE = "data.txt";
    private static final int MAX_ITEMS = 5;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<String> rssList = loadRssList();

        
        while (true) {
            System.out.println("Enter a number to choose an action:");
            System.out.println("1: Add new RSS feed");
            System.out.println("2: Remove RSS feed");
            System.out.println("3: Display latest RSS feed items");
            System.out.println("0: Exit");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    addRssFeed(scanner, rssList);
                    break;
                case 2:
                    removeRssFeed(scanner, rssList);
                    break;
                case 3:
                    displayRssItems(scanner, rssList);
                    break;
                case 0:
                    saveRssList(rssList);
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
            }
        }
    }

    private static List<String> loadRssList() {
        List<String> rssList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rssList.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading data file: " + e.getMessage());
        }
        return rssList;
    }

    private static void saveRssList(List<String> rssList) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (String rss : rssList) {
                writer.write(rss);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing to data file: " + e.getMessage());
        }
    }

    private static void addRssFeed(Scanner scanner, List<String> rssList) {
        System.out.println("Enter the name of the website:");
        String name = scanner.nextLine();
        System.out.println("Enter the URL of the website:");
        String url = scanner.nextLine();
        try {
            String rssUrl = extractRssUrl(url);
            String entry = name + ";" + url + ";" + rssUrl;
            rssList.add(entry);
            System.out.println("RSS feed added successfully.");
        } catch (IOException e) {
            System.out.println("Error extracting RSS URL: " + e.getMessage());
        }
    }

    private static void removeRssFeed(Scanner scanner, List<String> rssList) {
        System.out.println("Enter the name or URL of the website to remove:");
        String target = scanner.nextLine();
        boolean removed = rssList.removeIf(rss -> rss.contains(target));
        if (removed) {
            System.out.println("RSS feed removed successfully.");
        } else {
            System.out.println("No matching RSS feed found.");
        }
    }

    private static void displayRssItems(Scanner scanner, List<String> rssList) {
        System.out.println("Enter the name or URL of the website to display items:");
        String target = scanner.nextLine();
        for (String rss : rssList) {
            if (rss.contains(target)) {
                String[] parts = rss.split(";");
                if (parts.length == 3) {
                    String rssUrl = parts[2];
                    retrieveRssContent(rssUrl);
                }
            }
        }
    }

    public static String extractPageTitle(String html) {
        try {
            Document doc = Jsoup.parse(html);
            return doc.select("title").first().text();
        } catch (Exception e) {
            return "Error: no title tag found in page source!";
        }
    }

    public static String fetchPageSource(String urlString) throws Exception {
        URI uri = new URI(urlString);
        URL url = uri.toURL();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
        return toString(urlConnection.getInputStream());
    }

    private static String toString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(inputLine);
        }
        return stringBuilder.toString();
    }

    public static String extractRssUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        return doc.select("[type='application/rss+xml']").attr("abs:href");
    }

    public static void retrieveRssContent(String rssUrl) {
        try {
            String rssXml = fetchPageSource(rssUrl);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(rssXml.getBytes("UTF-8"));
            org.w3c.dom.Document doc = documentBuilder.parse(input);
            NodeList itemNodes = doc.getElementsByTagName("item");

            
            for (int i = 0; i < Math.min(MAX_ITEMS, itemNodes.getLength()); ++i) {
                Node itemNode = itemNodes.item(i);
                if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) itemNode;
                    System.out.println("Title: " + element.getElementsByTagName("title").item(0).getTextContent());
                    System.out.println("Link: " + element.getElementsByTagName("link").item(0).getTextContent());
                    System.out.println("Description: " + element.getElementsByTagName("description").item(0).getTextContent());
                }
            }
        } catch (Exception e) {
            System.out.println("Error in retrieving RSS content for " + rssUrl + ": " + e.getMessage());
        }
    }
}
