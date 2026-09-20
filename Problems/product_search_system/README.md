# Product Search System

## Problem Description

Design and implement an efficient product search system for an e-commerce platform. The system should support searching, filtering, and sorting products by various attributes including name, category, price, and tags.

## Design Decisions

### Architecture
- **Service-Oriented Design**: SearchService handles all search operations, separating concerns from data storage.
- **Stream API Usage**: Modern Java streams for filtering, sorting, and collecting results efficiently.
- **Immutable Product Model**: Product class with final fields for thread safety.
- **Flexible Search API**: Both individual methods and combined advanced search.

### Key Design Choices
- **In-Memory Storage**: Simple List for products; in production, would use database.
- **Case-Insensitive Matching**: User-friendly search for names and categories.
- **Tag-Based Search**: Multiple tags per product with OR logic.
- **Combined Filtering**: Advanced search method for multiple criteria.
- **Sorting Support**: Price-based sorting with ascending/descending options.

### Data Structures Used
- **ArrayList<Product>**: For product storage (dynamic, indexed access).
- **List<String>**: For product tags (flexible sizing).
- **Stream API**: For functional-style operations.

### Trade-offs
- **Memory vs. Performance**: In-memory storage fast but limited; database would scale better.
- **Simplicity vs. Features**: Basic implementation; advanced search could include fuzzy matching, scoring.
- **Streams vs. Loops**: Declarative style easier to read but may have overhead for small datasets.

## Features Implemented

1. **Search by product name** (partial matching, case-insensitive)
2. **Filter by category**
3. **Filter by price range**
4. **Search by tags**
5. **Sort results by price**

## How to Run

1. Compile: `javac com/example/lld/product_search_system/*.java`
2. Run: `java com.example.lld.product_search_system.Main`

## Example Output

```
=== Product Search System Demo ===

Search by name 'phone':
iPhone 15 (Electronics) - $999.99

Filter by category 'Electronics':
iPhone 15 (Electronics) - $999.99
MacBook Pro (Electronics) - $1999.99
Samsung TV (Electronics) - $799.99

...
```

## Extensions

- Add database persistence
- Implement fuzzy search with scoring
- Add pagination for large result sets
- Include user reviews and ratings in search
- Add caching for frequent queries
