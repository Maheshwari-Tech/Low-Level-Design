package com.mycompany.app.sales_analyics;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 * Sales Event (immutable)
 */
record SalesEvent(String productId, int unitsSold, LocalDate date) {
}

/**
 * In-Memory DB to store sales events
 */
class InMemoryDB {
    private final List<SalesEvent> events = new ArrayList<>();
    private final Map<LocalDate, Integer> dailyTotals = new HashMap<>();
    private final Map<String, Map<LocalDate, Integer>> productDayTotals = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void addEvent(SalesEvent event) {
        rwLock.writeLock().lock();
        try {
            events.add(event);
            dailyTotals.merge(event.date(), event.unitsSold(), Integer::sum);
            productDayTotals.computeIfAbsent(event.productId(), k -> new HashMap<>())
                    .merge(event.date(), event.unitsSold(), Integer::sum);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Map<LocalDate, Integer> getDailyTotals() {
        rwLock.readLock().lock();
        try {
            return new HashMap<>(dailyTotals);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Map<String, Map<LocalDate, Integer>> getProductDayTotals() {
        rwLock.readLock().lock();
        try {
            Map<String, Map<LocalDate, Integer>> copy = new HashMap<>();
            for (var e : productDayTotals.entrySet()) {
                copy.put(e.getKey(), new HashMap<>(e.getValue()));
            }
            return copy;
        } finally {
            rwLock.readLock().unlock();
        }
    }
}

/**
 * Analytics Service
 */
class AnalyticsService {
    private final InMemoryDB db;

    public AnalyticsService(InMemoryDB db) {
        this.db = db;
    }

    // 1. Maximum Sales in Window (K days)
    public int maxSalesInWindow(int K) {
        Map<LocalDate, Integer> daily = db.getDailyTotals();
        List<LocalDate> dates = new ArrayList<>(daily.keySet());
        Collections.sort(dates);

        int max = 0;
        for (int i = 0; i < dates.size(); i++) {
            int sum = 0;
            for (int j = i; j < dates.size() && j < i + K; j++) {
                sum += daily.get(dates.get(j));
            }
            max = Math.max(max, sum);
        }
        return max;
    }

    // 2. Moving Average of Sales (N days)
    public double movingAverage(int N) {
        Map<LocalDate, Integer> daily = db.getDailyTotals();
        List<LocalDate> dates = new ArrayList<>(daily.keySet());
        Collections.sort(dates);
        if (dates.size() < N) return 0;

        double sum = 0;
        for (int i = dates.size() - N; i < dates.size(); i++) {
            sum += daily.get(dates.get(i));
        }
        return sum / N;
    }

    // 3. Sales Spike Detection
    public List<LocalDate> detectSpikes(int N, double factor) {
        Map<LocalDate, Integer> daily = db.getDailyTotals();
        List<LocalDate> dates = new ArrayList<>(daily.keySet());
        Collections.sort(dates);
        List<LocalDate> spikes = new ArrayList<>();

        for (int i = N; i < dates.size(); i++) {
            double sum = 0;
            for (int j = i - N; j < i; j++) {
                sum += daily.get(dates.get(j));
            }
            double avg = sum / N;
            if (daily.get(dates.get(i)) > factor * avg) {
                spikes.add(dates.get(i));
            }
        }
        return spikes;
    }

    // 4. Longest Streak of Increasing Sales
    public int longestIncreasingStreak() {
        Map<LocalDate, Integer> daily = db.getDailyTotals();
        List<LocalDate> dates = new ArrayList<>(daily.keySet());
        Collections.sort(dates);
        if (dates.isEmpty()) return 0;

        int longest = 1, current = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (daily.get(dates.get(i)) > daily.get(dates.get(i - 1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }

    // 5. Top M Products by Sales Volume in last K days
    public List<Map.Entry<String, Integer>> topMProducts(int K, int M, LocalDate currentDate) {
        Map<String, Map<LocalDate, Integer>> productDay = db.getProductDayTotals();
        Map<String, Integer> totals = new HashMap<>();

        LocalDate start = currentDate.minusDays(K - 1);
        for (var e : productDay.entrySet()) {
            int sum = 0;
            for (var dayEntry : e.getValue().entrySet()) {
                if (!dayEntry.getKey().isBefore(start) && !dayEntry.getKey().isAfter(currentDate)) {
                    sum += dayEntry.getValue();
                }
            }
            totals.put(e.getKey(), sum);
        }

        PriorityQueue<Map.Entry<String, Integer>> heap = new PriorityQueue<>(Map.Entry.comparingByValue());
        for (var entry : totals.entrySet()) {
            heap.offer(entry);
            if (heap.size() > M) heap.poll();
        }

        List<Map.Entry<String, Integer>> result = new ArrayList<>(heap);
        result.sort((a, b) -> b.getValue() - a.getValue());
        return result;
    }
}

/**
 * Demo Runner
 */
public class solution {
    public static void main(String[] args) {
        InMemoryDB db = new InMemoryDB();
        AnalyticsService service = new AnalyticsService(db);

        // Sample events
        db.addEvent(new SalesEvent("A", 100, LocalDate.of(2025, 9, 1)));
        db.addEvent(new SalesEvent("B", 200, LocalDate.of(2025, 9, 1)));
        db.addEvent(new SalesEvent("A", 150, LocalDate.of(2025, 9, 2)));
        db.addEvent(new SalesEvent("C", 50, LocalDate.of(2025, 9, 3)));
        db.addEvent(new SalesEvent("B", 300, LocalDate.of(2025, 9, 4)));
        db.addEvent(new SalesEvent("A", 400, LocalDate.of(2025, 9, 5)));

        System.out.println("Max sales in 3-day window: " + service.maxSalesInWindow(3));
        System.out.println("Moving average (3 days): " + service.movingAverage(3));
        System.out.println("Spikes (N=2, factor=2): " + service.detectSpikes(2, 2.0));
        System.out.println("Longest increasing streak: " + service.longestIncreasingStreak());
        System.out.println("Top 2 products in last 3 days: " + service.topMProducts(3, 2, LocalDate.of(2025, 9, 5)));
    }
}
