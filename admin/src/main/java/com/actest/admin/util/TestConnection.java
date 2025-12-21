package com.actest.admin.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("Testing connection to localhost:8080...");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("test", "data");
            HttpClientUtil.post("/login", data);
            System.out.println("Request sent (unexpected success if server is down)");
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("Caught InterruptedException: " + e);
        } catch (Exception e) {
            System.out.println("Caught Exception: " + e);
            e.printStackTrace();
        }
    }
}
