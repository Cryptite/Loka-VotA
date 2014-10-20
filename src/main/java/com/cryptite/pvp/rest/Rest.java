package com.cryptite.pvp.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class Rest {
    private static final Logger log = Logger.getLogger("Rest");

    private static final String host = "http://loka.minecraftarium.com/api/";
//    private static final String host = "http://127.0.0.1:8000/api/";

    public static Boolean exists(String apiPoint, String name) {

        try {
            HttpURLConnection conn = getConnection(apiPoint, name, "GET");
            int response = conn.getResponseCode();
            conn.disconnect();

            return response == 200;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Boolean put(String apiPoint, String name, String json) {
        HttpURLConnection conn = getConnection(apiPoint, name, "PUT");
        return write(conn, json);
    }

    public static Boolean post(String apiPoint, String name, String json) {
        HttpURLConnection conn = getConnection(apiPoint, name, "POST");
        return write(conn, json);
    }

    private static boolean write(HttpURLConnection conn, String json) {
        try {
            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            int response = conn.getResponseCode();

            //Output, if needed for whatever reason
            String output;
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
            if (response != 200 && response != 201) {
                log.info("Output from Server .... \n");
                while ((output = br.readLine()) != null) {
                    log.info(output);
                }
                log.info("Json: " + json);
            }

            conn.disconnect();
            return response == 200 || response == 201;

        } catch (MalformedURLException e) {
//            log.info("MalformedURLException with Json: " + json);
            e.printStackTrace();
            return false;

        } catch (IOException e) {
//            log.info("IOException with Json: " + json);
            e.printStackTrace();
            return false;
        }
    }

    private static HttpURLConnection getConnection(String apiPoint, String name, String method) {
        try {
            String urlString = host + apiPoint + "/";
            if (name != null) urlString += name + "/";
//            log.info("url: " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            log.info("URL is: " + urlString);
            if (method.equals("GET")) {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
            } else if (method.equals("PUT") || method.equals("POST")) {
                conn.setDoOutput(true);
                conn.setRequestMethod(method);
                conn.setRequestProperty("Content-Type", "application/json");
            }

            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
