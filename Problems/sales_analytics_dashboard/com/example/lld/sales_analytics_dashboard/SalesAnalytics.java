package com.example.lld.sales_analytics_dashboard;

import java.util.*;

/**
 * Sales Analytics Dashboard
 *
 * Design Choices:
 * - Deque for sliding window operations (O(1) add/remove).
 * - HashMap for product sales tracking.
 * - Real-time processing with update methods.
 */
public class SalesAnalytics {
    private Deque<Double> dailySales;
    private Map<String, Integer> productSales;
    private int windowSize;
    private double currentSum;

    public SalesAnalytics(int windowSize) {
        this.windowSize = windowSize;
        this.dailySales = new LinkedList<>();
        this.productSales = new HashMap<>();
        this.currentSum = 0.0;
    }

    /**
     * Add daily sales data
     */
    public void addDailySales(double sales) {
        dailySales.addLast(sales);
        currentSum += sales;

        // Maintain window size
        if (dailySales.size() > windowSize) {
            double removed = dailySales.removeFirst();
            currentSum -= removed;
        }
    }

    /**
     * Add product sales
     */
    public void addProductSales(String product, int quantity) {
        productSales.put(product, productSales.getOrDefault(product, 0) + quantity);
    }

    /**
     * 1. Maximum Sales in Window (K days)
     */
    public double getMaxSalesInWindow() {
        return dailySales.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    /**
     * 2. Moving Average of Sales (N days)
     */
    public double getMovingAverage() {
        return dailySales.isEmpty() ? 0.0 : currentSum / dailySales.size();
    }

    /**
     * 3. Sales Spike Detection
     */
    public boolean detectSpike(double sales) {
        double avg = getMovingAverage();
        return sales > 2 * avg;
    }

    /**
     * 4. Longest Streak of Increasing Sales
     */
    public int getLongestIncreasingStreak() {
        if (dailySales.size() < 2) return 0;

        int maxStreak = 0;
        int currentStreak = 1;
        Double prev = null;

        for (Double sales : dailySales) {
            if (prev != null && sales > prev) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
            prev = sales;
        }

        return maxStreak;
    }

    /**
     * 5. Top M Products by Sales Volume (last K days)
     */
    public List<Map.Entry<String, Integer>> getTopProducts(int m) {
        return productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(m)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
}
