# Sales Analytics Dashboard

## Problem Description

Design and implement a Sales Analytics Dashboard that processes and analyzes real-time streaming sales data for an e-commerce platform.

## Features

1. **Maximum Sales in Window (K days)**: Identify the highest sales amount within a window of K days.
2. **Moving Average of Sales (N days)**: Calculate the moving average of sales over the last N days.
3. **Sales Spike Detection**: Detect spikes where sales exceed twice the moving average within a specified window.
4. **Longest Streak of Increasing Sales**: Track the longest sequence of consecutive days with increasing sales.
5. **Top M Products by Sales Volume**: Retrieve the top M products based on sales volume within the last K days.

## Design Decisions

### Architecture
- **Sliding Window Pattern**: Deque for efficient O(1) window operations.
- **Real-time Processing**: Incremental updates for sums and statistics.
- **Separate Data Structures**: Deque for time-series sales, Map for product aggregation.

### Key Choices
- **Deque for Window**: LinkedList implementation for fast add/remove operations.
- **Running Sum**: Maintain current sum for O(1) average calculations.
- **HashMap for Products**: O(1) access for product sales tracking.
- **Stream API**: For sorting and limiting top products.

### Data Structures
- **Deque<Double>**: Sales window (FIFO, bounded size).
- **HashMap<String, Integer>**: Product sales volume.
- **Primitive double**: Running sum for efficiency.

### Trade-offs
- **Memory vs. Speed**: Deque stores all window values; could use circular buffer for memory optimization.
- **Accuracy vs. Performance**: Exact calculations vs. approximate for very large datasets.
- **In-memory vs. Persistent**: Current design is memory-based; production would need database.

## How to Run

1. Compile: `javac com/example/lld/sales_analytics_dashboard/*.java`
2. Run: `java com.example.lld.sales_analytics_dashboard.Main`

## Example Output

```
=== Sales Analytics Dashboard ===

1. Max sales in window: $300.0
2. Moving average: $199.00
3. Spike detected for $300: true
4. Longest increasing streak: 3 days
5. Top 3 products:
   AirPods: 60 units
   iPhone: 50 units
   iPad: 40 units
```

## Extensions

- Add time-based windows with timestamps
- Implement database persistence
- Add real-time streaming with Kafka
- Include seasonal trend analysis
- Add alert system for spikes
