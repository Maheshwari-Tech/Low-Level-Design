package com.mycompany.app;

import com.mycompany.app.models.LevelCacheData;
import com.mycompany.app.models.ReadResponse;
import com.mycompany.app.models.UsageResponse;
import com.mycompany.app.models.WriteResponse;

import java.util.ArrayList;
import java.util.List;

public class DefaultLevelCache<Key, Value> implements ILevelCache<Key, Value>{
    LevelCacheData levelCacheData;

    DefaultLevelCache(LevelCacheData levelCacheData, Cache<Key, Value> cache, ILevelCache<Key, Value> next){
        this.levelCacheData = levelCacheData;
        this.cache = cache;
        this.next = next;
    }

    Cache<Key, Value> cache;
    ILevelCache<Key, Value> next;

    @Override
    public ReadResponse<Value> read(Key key){
        double timeTaken = levelCacheData.readTime();
        Value val = cache.get(key);

        if(val == null){
            ReadResponse<Value> nextResponse = next.read(key);
            val = nextResponse.value();
            timeTaken += nextResponse.timeTaken();

            if(val != null){
               cache.put(key, val);
               timeTaken += levelCacheData.writeTime();
            }
        }
        return new ReadResponse<>(val, timeTaken);
    }

    @Override
    public WriteResponse write(Key key, Value value){

        double timeTaken = levelCacheData.readTime();
        Value val = cache.get(key);

        if(val == null || val != value){
            timeTaken += levelCacheData.writeTime();
            cache.put(key, value);

            WriteResponse nextResponse = next.write(key, value);
            timeTaken += nextResponse.time();
        }

        return new WriteResponse(timeTaken);
    }

    @Override
    public List<UsageResponse> usageStats(){
        List<UsageResponse> usageResponses = next.usageStats();
        UsageResponse currentUsage = new UsageResponse(cache.size(), levelCacheData.capacity());
        usageResponses.add(currentUsage);
        return usageResponses;
    }
}
