
## Problem Overview

Design and implement a shopping cart system where each item has a time-to-live (TTL) expiration. The system should:
1. Add items to the cart, associating each with a TTL (time-to-live).
2. Automatically remove expired items.
3. Retrieve the current cart contents, excluding expired items.
4. Calculate the total price of all valid (non-expired) items.
5. Ensure thread-safety for concurrent access.
