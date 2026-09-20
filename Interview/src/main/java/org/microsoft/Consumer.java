package org.microsoft;

public interface Consumer {
    Status consume(int partition_id);
}
