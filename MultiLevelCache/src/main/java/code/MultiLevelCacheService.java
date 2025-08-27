package code;

import code.exception.NotFoundException;
import code.models.ReadResponse;
import code.models.UsageResponse;
import code.models.WriteResponse;

import java.util.*;

public class MultiLevelCacheService<Key, Value>{

    private final ILevelCache<Key, Value> head;
    private final int lastK;
    Deque<Double> readTimes;
    Deque<Double> writeTimes;

    MultiLevelCacheService(final int lastK, final ILevelCache<Key, Value> head){
        this.head = head;
        this.lastK = lastK;
        readTimes = new ArrayDeque<>();
        writeTimes = new ArrayDeque<>();
    }

    public Value read(Key key) {
        ReadResponse<Value> readResponse = head.read(key);

        readTimes.addLast(readResponse.timeTaken());
        if(readTimes.size() > lastK){
            readTimes.removeFirst();
        }
        return readResponse.value();
    }

    public void write(Key key, Value value) {
        WriteResponse writeResponse = head.write(key, value);

        writeTimes.addLast(writeResponse.time());
        if(writeTimes.size() > lastK){
            writeTimes.removeFirst();
        }
    }

    public double getAvgReadTime(){
        if(readTimes.isEmpty()){
            throw new NotFoundException("no read found");
        }
        return getSum(readTimes)/readTimes.size();
    }

    public double getAvgWriteTime(){
        if(writeTimes.isEmpty()){
            throw new NotFoundException("no writes found");
        }
        return getSum(writeTimes)/writeTimes.size();
    }

    private double getSum(Collection<Double> arr){
        double sum = 0;
        for(double t: readTimes){
            sum+= t;
        }
        return sum;
    }
    public List<UsageResponse> usageStats(){
        List<UsageResponse> responses = head.usageStats();
        Collections.reverse(responses);
       return responses;
    }
}
