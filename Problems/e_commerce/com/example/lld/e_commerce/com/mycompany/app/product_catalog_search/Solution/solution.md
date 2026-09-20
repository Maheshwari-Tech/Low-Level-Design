import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* Complete, working, and reasonably optimal in-memory product search engine.
* Features:
* 1) Partial name search (prefix on tokens)
* 2) Filter by category
* 3) Filter by price range
* 4) Search by tags with AND/OR support
* 5) Sort by price or relevance
*
* Implementation notes:
* - Inverted indices for name prefixes, category, tags
* - Price index via TreeMap for efficient range queries
* - Candidate selection uses the smallest posting list first (reduces intersection cost)
* - Relevance score favors name hits, then tag hits, then category match; ties broken by price asc, id asc
* - Thread-safety: this sample is not synchronized; wrap with external synchronization if used concurrently
    */
    public class ProductSearchDemo {

/* ===== Domain Models ===== */

public static final class Product {
private final int id;
private final String name;         // tokenized by whitespace
private final String category;     // normalized to lowercase
private final double price;        // currency agnostic for demo
private final Set<String> tags;    // normalized to lowercase

     public Product(int id, String name, String category, double price, Collection<String> tags) {
         this.id = id;
         this.name = Objects.requireNonNull(name);
         this.category = normalize(category);
         this.price = price;
         this.tags = tags == null ? Set.of() : Set.copyOf(tags.stream().map(ProductSearchDemo::normalize).collect(Collectors.toSet()));
     }

     public int getId() { return id; }
     public String getName() { return name; }
     public String getCategory() { return category; }
     public double getPrice() { return price; }
     public Set<String> getTags() { return tags; }

     @Override public String toString() {
         return "Product{" +
                 "id=" + id +
                 ", name='" + name + '\'' +
                 ", category='" + category + '\'' +
                 ", price=" + price +
                 ", tags=" + tags +
                 '}';
     }
}

public enum SortBy { PRICE_ASC, PRICE_DESC, RELEVANCE }
public enum TagOperator { AND, OR }

public static final class SearchQuery {
private final String namePrefix;    // nullable; case-insensitive
private final String category;      // nullable; case-insensitive
private final Double minPrice;      // nullable
private final Double maxPrice;      // nullable
private final List<String> tags;    // nullable; case-insensitive
private final TagOperator tagOperator; // AND or OR, default OR
private final SortBy sortBy;        // default RELEVANCE
private final Integer limit;        // nullable (no limit)
private final Integer offset;       // nullable (0)

     private SearchQuery(Builder b) {
         this.namePrefix = isBlank(b.namePrefix) ? null : normalize(b.namePrefix);
         this.category = isBlank(b.category) ? null : normalize(b.category);
         this.minPrice = b.minPrice;
         this.maxPrice = b.maxPrice;
         this.tags = b.tags == null ? null : b.tags.stream().filter(s -> !isBlank(s)).map(ProductSearchDemo::normalize).toList();
         this.tagOperator = b.tagOperator == null ? TagOperator.OR : b.tagOperator;
         this.sortBy = b.sortBy == null ? SortBy.RELEVANCE : b.sortBy;
         this.limit = b.limit;
         this.offset = b.offset == null ? 0 : Math.max(0, b.offset);
     }

     public static class Builder {
         private String namePrefix;
         private String category;
         private Double minPrice;
         private Double maxPrice;
         private List<String> tags;
         private TagOperator tagOperator;
         private SortBy sortBy;
         private Integer limit;
         private Integer offset;

         public Builder namePrefix(String s) { this.namePrefix = s; return this; }
         public Builder category(String s) { this.category = s; return this; }
         public Builder minPrice(Double d) { this.minPrice = d; return this; }
         public Builder maxPrice(Double d) { this.maxPrice = d; return this; }
         public Builder priceRange(Double min, Double max) { this.minPrice = min; this.maxPrice = max; return this; }
         public Builder tags(List<String> t) { this.tags = t; return this; }
         public Builder tagOperator(TagOperator op) { this.tagOperator = op; return this; }
         public Builder sortBy(SortBy s) { this.sortBy = s; return this; }
         public Builder limit(Integer n) { this.limit = n; return this; }
         public Builder offset(Integer n) { this.offset = n; return this; }
         public SearchQuery build() { return new SearchQuery(this); }
     }

     public Optional<String> namePrefix() { return Optional.ofNullable(namePrefix); }
     public Optional<String> category() { return Optional.ofNullable(category); }
     public Optional<Double> minPrice() { return Optional.ofNullable(minPrice); }
     public Optional<Double> maxPrice() { return Optional.ofNullable(maxPrice); }
     public Optional<List<String>> tags() { return Optional.ofNullable(tags); }
     public TagOperator tagOperator() { return tagOperator; }
     public SortBy sortBy() { return sortBy; }
     public Optional<Integer> limit() { return Optional.ofNullable(limit); }
     public int offset() { return offset == null ? 0 : offset; }
}

/* ===== Index: posting lists over product ids ===== */

public interface SearchIndex {
void add(Product p);
void remove(int productId);
Optional<Product> get(int productId);
Collection<Product> allProducts();

     IntSet byNamePrefix(String prefix);
     IntSet byCategory(String category);
     IntSet byAnyTag(Collection<String> tags); // OR
     IntSet byAllTags(Collection<String> tags); // AND
     IntSet byPriceRange(Double min, Double max);
}

public static final class InMemoryIndex implements SearchIndex {
private final Map<Integer, Product> products = new HashMap<>();

     // name prefix -> posting list of product ids
     private final Map<String, IntSet> namePrefixIndex = new HashMap<>();
     // category -> posting list
     private final Map<String, IntSet> categoryIndex = new HashMap<>();
     // tag -> posting list
     private final Map<String, IntSet> tagIndex = new HashMap<>();
     // price -> posting list (for duplicates). TreeMap enables efficient range queries
     private final NavigableMap<Double, IntSet> priceIndex = new TreeMap<>();

     @Override public void add(Product p) {
         products.put(p.getId(), p);
         // index name prefixes per token
         for (String tok : tokenize(p.getName())) {
             addAllPrefixes(tok, p.getId());
         }
         // category
         categoryIndex.computeIfAbsent(p.getCategory(), k -> new IntSet()).add(p.getId());
         // tags
         for (String t : p.getTags()) {
             tagIndex.computeIfAbsent(t, k -> new IntSet()).add(p.getId());
         }
         // price
         priceIndex.computeIfAbsent(p.getPrice(), k -> new IntSet()).add(p.getId());
     }

     @Override public void remove(int productId) {
         Product p = products.remove(productId);
         if (p == null) return;
         // remove from name prefixes
         for (String tok : tokenize(p.getName())) {
             for (int i = 1; i <= tok.length(); i++) {
                 String pref = tok.substring(0, i);
                 IntSet s = namePrefixIndex.get(pref);
                 if (s != null) { s.remove(productId); if (s.isEmpty()) namePrefixIndex.remove(pref); }
             }
         }
         // category
         removePosting(categoryIndex, p.getCategory(), productId);
         // tags
         for (String t : p.getTags()) removePosting(tagIndex, t, productId);
         // price
         IntSet s = priceIndex.get(p.getPrice());
         if (s != null) { s.remove(productId); if (s.isEmpty()) priceIndex.remove(p.getPrice()); }
     }

     @Override public Optional<Product> get(int productId) { return Optional.ofNullable(products.get(productId)); }
     @Override public Collection<Product> allProducts() { return products.values(); }

     @Override public IntSet byNamePrefix(String prefix) {
         if (isBlank(prefix)) return new IntSet(products.keySet());
         IntSet s = namePrefixIndex.get(normalize(prefix));
         return s == null ? IntSet.empty() : s.copy();
     }

     @Override public IntSet byCategory(String category) {
         if (isBlank(category)) return new IntSet(products.keySet());
         IntSet s = categoryIndex.get(normalize(category));
         return s == null ? IntSet.empty() : s.copy();
     }

     @Override public IntSet byAnyTag(Collection<String> tags) {
         if (tags == null || tags.isEmpty()) return new IntSet(products.keySet());
         IntSet out = new IntSet();
         for (String t : tags) {
             IntSet s = tagIndex.get(normalize(t));
             if (s != null) out.unionInPlace(s);
         }
         return out;
     }

     @Override public IntSet byAllTags(Collection<String> tags) {
         if (tags == null || tags.isEmpty()) return new IntSet(products.keySet());
         // Start with smallest posting to minimize intersections
         List<IntSet> lists = new ArrayList<>();
         for (String t : tags) {
             IntSet s = tagIndex.get(normalize(t));
             if (s == null) return IntSet.empty();
             lists.add(s);
         }
         lists.sort(Comparator.comparingInt(IntSet::size));
         IntSet acc = lists.get(0).copy();
         for (int i = 1; i < lists.size(); i++) acc.intersectInPlace(lists.get(i));
         return acc;
     }

     @Override public IntSet byPriceRange(Double min, Double max) {
         if (products.isEmpty()) return IntSet.empty();
         Double lo = min == null ? priceIndex.firstKey() : min;
         Double hi = max == null ? priceIndex.lastKey() : max;
         if (lo > hi) return IntSet.empty();
         IntSet out = new IntSet();
         for (Map.Entry<Double, IntSet> e : priceIndex.subMap(lo, true, hi, true).entrySet()) {
             out.unionInPlace(e.getValue());
         }
         return out;
     }

     private void addAllPrefixes(String token, int productId) {
         for (int i = 1; i <= token.length(); i++) {
             String pref = token.substring(0, i);
             namePrefixIndex.computeIfAbsent(pref, k -> new IntSet()).add(productId);
         }
     }

     private static void removePosting(Map<String, IntSet> map, String key, int id) {
         IntSet s = map.get(key);
         if (s != null) { s.remove(id); if (s.isEmpty()) map.remove(key); }
     }
}

/* ===== IntSet: memory-efficient int posting list with fast ops ===== */
public static final class IntSet {
private final Set<Integer> s;
public IntSet() { this.s = new HashSet<>(); }
public IntSet(Collection<Integer> c) { this.s = new HashSet<>(c); }
public static IntSet empty() { return new IntSet(); }
public void add(int x) { s.add(x); }
public void remove(int x) { s.remove(x); }
public boolean isEmpty() { return s.isEmpty(); }
public int size() { return s.size(); }
public IntSet copy() { return new IntSet(s); }
public void unionInPlace(IntSet other) { if (other != null) s.addAll(other.s); }
public void intersectInPlace(IntSet other) { s.retainAll(other.s); }
public Set<Integer> asSet() { return s; }
public Iterator<Integer> iterator() { return s.iterator(); }
}

/* ===== Search Service ===== */
public interface SearchService {
List<Product> search(SearchQuery query);
void add(Product p);
void remove(int productId);
}

public static final class CatalogSearchService implements SearchService {
private final SearchIndex index;
private final RelevanceScorer scorer;

     public CatalogSearchService(SearchIndex index) { this(index, new RelevanceScorer()); }
     public CatalogSearchService(SearchIndex index, RelevanceScorer scorer) {
         this.index = index; this.scorer = scorer;
     }

     @Override public void add(Product p) { index.add(p); }
     @Override public void remove(int productId) { index.remove(productId); }

     @Override public List<Product> search(SearchQuery q) {
         // Build candidate postings per active constraint
         List<IntSet> constraints = new ArrayList<>();

         q.namePrefix().ifPresent(pref -> constraints.add(index.byNamePrefix(pref)));
         q.category().ifPresent(cat -> constraints.add(index.byCategory(cat)));

         // tags
         if (q.tags().isPresent()) {
             List<String> t = q.tags().get();
             constraints.add(q.tagOperator() == TagOperator.AND ? index.byAllTags(t) : index.byAnyTag(t));
         }

         // price
         if (q.minPrice().isPresent() || q.maxPrice().isPresent()) {
             constraints.add(index.byPriceRange(q.minPrice().orElse(null), q.maxPrice().orElse(null)));
         }

         // If no constraints, start from ALL
         IntSet candidates;
         if (constraints.isEmpty()) {
             candidates = new IntSet(index.allProducts().stream().map(Product::getId).collect(Collectors.toSet()));
         } else {
             // intersect starting from smallest list for efficiency
             constraints.sort(Comparator.comparingInt(IntSet::size));
             candidates = constraints.get(0).copy();
             for (int i = 1; i < constraints.size(); i++) candidates.intersectInPlace(constraints.get(i));
         }

         if (candidates.isEmpty()) return List.of();

         // Load products and score for relevance
         List<Product> items = new ArrayList<>(candidates.size());
         Map<Integer, Double> score = new HashMap<>();
         for (int id : candidates.asSet()) {
             Product p = index.get(id).orElse(null);
             if (p != null) {
                 items.add(p);
                 score.put(id, scorer.score(p, q));
             }
         }

         // Sort
         Comparator<Product> cmp;
         switch (q.sortBy()) {
             case PRICE_ASC -> cmp = Comparator.comparingDouble(Product::getPrice).thenComparingInt(Product::getId);
             case PRICE_DESC -> cmp = Comparator.comparingDouble(Product::getPrice).reversed().thenComparingInt(Product::getId);
             default -> cmp = Comparator.<Product>comparingDouble(p -> -score.getOrDefault(p.getId(), 0.0))
                     .thenComparingDouble(Product::getPrice)
                     .thenComparingInt(Product::getId);
         }
         items.sort(cmp);

         // Pagination (optional)
         int from = Math.min(q.offset(), items.size());
         int to = q.limit().map(l -> Math.min(from + Math.max(0, l), items.size())).orElse(items.size());
         return items.subList(from, to);
     }
}

/* ===== Relevance Scorer ===== */
public static final class RelevanceScorer {
// weights are tunable; emphasize name matches the most
private static final double W_NAME_TOKEN_PREFIX = 3.0;
private static final double W_NAME_FULL_TOKEN = 2.0;
private static final double W_TAG_HIT = 1.5;
private static final double W_CATEGORY_MATCH = 1.0;

     public double score(Product p, SearchQuery q) {
         double s = 0.0;
         if (q.namePrefix().isPresent()) {
             String pref = q.namePrefix().get();
             for (String tok : tokenize(p.getName())) {
                 if (tok.startsWith(pref)) s += W_NAME_TOKEN_PREFIX;
                 if (tok.equals(pref)) s += W_NAME_FULL_TOKEN; // exact token boost
             }
         }
         if (q.tags().isPresent()) {
             Set<String> tagSet = p.getTags();
             for (String t : q.tags().get()) {
                 if (tagSet.contains(t)) s += W_TAG_HIT;
             }
         }
         if (q.category().isPresent() && p.getCategory().equals(q.category().get())) s += W_CATEGORY_MATCH;
         return s;
     }
}

/* ===== Utilities ===== */
private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
private static String normalize(String s) { return s == null ? null : s.trim().toLowerCase(Locale.ROOT); }
private static List<String> tokenize(String name) {
if (isBlank(name)) return List.of();
return Arrays.stream(name.toLowerCase(Locale.ROOT).split("\\s+"))
.filter(t -> !t.isEmpty()).toList();
}

/* ===== Demo main ===== */
public static void main(String[] args) {
SearchIndex index = new InMemoryIndex();
CatalogSearchService service = new CatalogSearchService(index);

     service.add(new Product(1, "Apple iPhone 15 Pro", "Smartphones", 999.0, List.of("ios", "flagship", "camera")));
     service.add(new Product(2, "Samsung Galaxy S24", "Smartphones", 899.0, List.of("android", "flagship")));
     service.add(new Product(3, "Apple Watch Ultra", "Wearables", 799.0, List.of("watch", "ios")));
     service.add(new Product(4, "Noise ColorFit Pro", "Wearables", 59.0, List.of("watch", "budget")));
     service.add(new Product(5, "Dell XPS 13 Laptop", "Laptops", 1199.0, List.of("windows", "ultrabook")));
     service.add(new Product(6, "Apple MacBook Air 13", "Laptops", 1099.0, List.of("macos", "ultrabook")));

     // Example: name prefix = "app", category = smartphones, price <= 1000, tags ANY of [flagship, camera], sort by relevance
     SearchQuery q1 = new SearchQuery.Builder()
             .namePrefix("app")
             .category("smartphones")
             .maxPrice(1000.0)
             .tags(List.of("flagship", "camera"))
             .tagOperator(TagOperator.OR)
             .sortBy(SortBy.RELEVANCE)
             .build();

     System.out.println("\nQuery #1: " + service.search(q1));

     // Example: tags AND [watch, ios], any category, price range [100, 900], sort by price asc
     SearchQuery q2 = new SearchQuery.Builder()
             .tags(List.of("watch", "ios"))
             .tagOperator(TagOperator.AND)
             .priceRange(100.0, 900.0)
             .sortBy(SortBy.PRICE_ASC)
             .build();

     System.out.println("\nQuery #2: " + service.search(q2));

     // Example: name prefix "lap", category laptops, OR tags [ultrabook, macos], sort by price desc
     SearchQuery q3 = new SearchQuery.Builder()
             .namePrefix("lap")
             .category("laptops")
             .tags(List.of("ultrabook", "macos"))
             .tagOperator(TagOperator.OR)
             .sortBy(SortBy.PRICE_DESC)
             .build();

     System.out.println("\nQuery #3: " + service.search(q3));
}
}
