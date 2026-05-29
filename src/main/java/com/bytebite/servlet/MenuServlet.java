package com.bytebite.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Menu Servlet
 * Handles menu operations with caching support
 */
@WebServlet(urlPatterns = {"/menu", "/api/menu"}, asyncSupported = true)
public class MenuServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MenuServlet.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    private List<Map<String, Object>> menuCache;
    private long cacheTime;
    private long cacheTTL;

    @Override
    public void init() throws ServletException {
        super.init();
        String cacheTTLParam = getInitParameter("cache.ttl.minutes");
        cacheTTL = (cacheTTLParam != null ? Long.parseLong(cacheTTLParam) : 30) * 60 * 1000;
        logger.info("MenuServlet initialized with cache TTL: {} minutes", cacheTTL / 60000);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if (path.contains("api")) {
            handleAPIRequest(request, response);
        } else {
            handleWebRequest(request, response);
        }
    }

    private void handleWebRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().println(getMenuPageHTML(request));
    }

    private void handleAPIRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        List<Map<String, Object>> menu = getMenu();
        response.getWriter().write(objectMapper.writeValueAsString(menu));
    }

    private synchronized List<Map<String, Object>> getMenu() {
        long now = System.currentTimeMillis();
        
        // Check if cache is valid
        if (menuCache != null && (now - cacheTime) < cacheTTL) {
            logger.debug("Returning cached menu");
            return menuCache;
        }

        logger.info("Fetching menu from database");
        menuCache = buildMenu();
        cacheTime = now;
        return menuCache;
    }

    private List<Map<String, Object>> buildMenu() {
        List<Map<String, Object>> menu = new ArrayList<>();

        // Sample menu items
        String[] items = {"Pizza", "Burger", "Pasta", "Salad", "Dessert", "Drink"};
        double[] prices = {12.99, 10.99, 11.99, 8.99, 5.99, 2.99};

        for (int i = 0; i < items.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i + 1);
            item.put("name", items[i]);
            item.put("price", prices[i]);
            item.put("available", true);
            menu.add(item);
        }

        return menu;
    }

    private String getMenuPageHTML(HttpServletRequest request) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<title>ByteBite Menu</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 20px; }\n");
        html.append(".container { max-width: 1200px; margin: 0 auto; }\n");
        html.append("h1 { text-align: center; color: #333; }\n");
        html.append(".menu-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 20px; margin-top: 40px; }\n");
        html.append(".menu-item { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n");
        html.append(".menu-item h3 { margin-top: 0; color: #667eea; }\n");
        html.append(".price { font-size: 18px; font-weight: bold; color: #764ba2; }\n");
        html.append(".nav { text-align: right; margin-bottom: 20px; }\n");
        html.append(".nav a { margin-left: 15px; text-decoration: none; color: #667eea; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='container'>\n");
        html.append("<div class='nav'><a href='").append(request.getContextPath()).append("/logout'>Logout</a></div>\n");
        html.append("<h1>ByteBite Menu</h1>\n");
        html.append("<div class='menu-grid'>\n");

        for (Map<String, Object> item : getMenu()) {
            html.append("<div class='menu-item'>\n");
            html.append("<h3>").append(item.get("name")).append("</h3>\n");
            html.append("<p class='price'>$").append(String.format("%.2f", item.get("price"))).append("</p>\n");
            html.append("<button>Add to Cart</button>\n");
            html.append("</div>\n");
        }

        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }
}
