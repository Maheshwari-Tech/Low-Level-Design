## Problem Statement

Design an order processing system for an e-commerce platform.
Orders have priorities (URGENT, HIGH, NORMAL, LOW) 
and should be processed in priority order, FIFO within the same priority.
Implement the following operations: 

1. Add order with priority
2. Process next order(highest priority first)
3. Cancel specific orderby ID
4. Get orders by priority level
5. Get estimated processing time for an order