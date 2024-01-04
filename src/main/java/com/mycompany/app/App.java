//Meza Benitez David Emanuel 4CM12

package com.mycompany.app;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;


public class App {

    public static void main(String[] args) throws IOException {
        int port = 80;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/search", new MyHandler());
        server.setExecutor(null);
        server.createContext("/monitor", new MonitorHandler());
        server.start();
        System.out.println("Server started on port " + port);
    }
    static class MonitorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    
            // Obtener el uso de CPU y memoria
            System.out.println(osBean.getCpuLoad() * 100 + "%"); // still always 0%
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            double cpuLoad = osBean.getCpuLoad() * 100;
            long totalMemory = osBean.getTotalMemorySize();
            long freeMemory = osBean.getFreeMemorySize();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory * 100 / totalMemory;
    
            // Crear la respuesta
            String response = "{"+ "\n" +
                                "\"CPU\": " + cpuLoad + "\n" +
                              "\"Memoria\": " + memoryUsage + "\n" +
                              " }";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String query = reader.readLine();
                int partition = Integer.parseInt(reader.readLine());
                
                Map<String, Object[]> fileScores = processFiles(query.split("\\s+"), partition);
                StringBuilder jsonResponse = new StringBuilder("{\n");
                int[] presenceCount = null;
                for (Map.Entry<String, Object[]> entry : fileScores.entrySet()) {
                    System.out.println("presenceCount.equals(entry.getKey()");
                    if ("presenceCount".equals(entry.getKey())) {
                        presenceCount = (int[]) entry.getValue()[0];
                        continue;
                    }
            
                    double[] scores = (double[]) entry.getValue()[0];
                    jsonResponse.append(String.format("  \"%s\": {\"scores\": %s},\n", entry.getKey(), Arrays.toString(scores)));
                }
            
                // Agregar presenceCount al JSON
                if (presenceCount != null) {
                    jsonResponse.append(String.format("  \"presenceCount\": %s,\n", Arrays.toString(presenceCount)));
                }
            
                if (jsonResponse.length() > 2) {
                    jsonResponse.deleteCharAt(jsonResponse.length() - 2); // Remove last comma
                }
                jsonResponse.append("}");
            
                System.err.println(jsonResponse);

                String response = jsonResponse.toString();
                t.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private static Map<String, Object[]> processFiles(String[] queries, int partition) {
        File folder = new File("/Users/davidmeza/Documents/Escuela /Proyecto/TextMiningServer/src/main/java/com/mycompany/app/LIBROS_TXT");
        File[] listOfFiles = folder.listFiles();
        Arrays.sort(listOfFiles); // Sort files for consistent partitioning
        int totalFiles = listOfFiles.length;
        int partSize = totalFiles / 3;
        int start = (partition - 1) * partSize;
        int end = (partition == 3) ? totalFiles : start + partSize;

        Map<String, Object[]> fileScores = new HashMap<>();
    int[] presenceCount = new int[queries.length];
    Arrays.fill(presenceCount, 0);

    for (int i = start; i < end; i++) {
        File file = listOfFiles[i];
        if (file.isFile()) {
            int totalWords = countTotalWords(file);
            double[] scores = new double[queries.length];
            for (int j = 0; j < queries.length; j++) {
                double occurrences = countOccurrences(file, queries[j]);
                scores[j] = (totalWords > 0) ? occurrences / (double) totalWords : 0;
                if (occurrences > 0) {
                    presenceCount[j]++;
                }
            }
            fileScores.put(file.getName(), new Object[] {scores});
        }
    }

    // Añadir el conteo de presencia a la respuesta
    fileScores.put("presenceCount", new Object[] {presenceCount});
    return fileScores;
    }
    private static int countTotalWords(File file) {
        int totalWords = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalWords += line.split("\\s+").length;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalWords;
    }
    
    private static int countOccurrences(File file, String query) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                count += countMatches(line, query);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    private static int countMatches(String text, String query) {
        return text.split(query, -1).length - 1;
    }
}
