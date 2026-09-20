package com.example.lld.sales_analytics_dashboard;

public class Main {
    public static void main(String[] args) {
        SalesAnalytics analytics = new SalesAnalytics(7); // 7-day window

        // Add sample sales data
        double[] sales = {100, 120, 90, 150, 200, 180, 250, 300, 220, 280};
        for (double sale : sales) {
            analytics.addDailySales(sale);
        }

        // Add product sales
        analytics.addProductSales("iPhone", 50);
        analytics.addProductSales("MacBook", 30);
        analytics.addProductSales("iPad", 40);
        analytics.addProductSales("AirPods", 60);

        System.out.println("=== Sales Analytics Dashboard ===\n");

        System.out.println("1. Max sales in window: $" + analytics.getMaxSalesInWindow());
        System.out.println("2. Moving average: $" + String.format("%.2f", analytics.getMovingAverage()));
        System.out.println("3. Spike detected for $300: " + analytics.detectSpike(300));
        System.out.println("4. Longest increasing streak: " + analytics.getLongestIncreasingStreak() + " days");
        System.out.println("5. Top 3 products:");
        analytics.getTopProducts(3).forEach(entry ->
            System.out.println("   " + entry.getKey() + ": " + entry.getValue() + " units"));
    }
}
