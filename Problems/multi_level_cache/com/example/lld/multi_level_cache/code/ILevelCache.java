package code;

import code.models.ReadResponse;
import code.models.UsageResponse;
import code.models.WriteResponse;

import java.util.List;

public interface ILevelCache<Key, Value> {

    ReadResponse<Value> read(Key key);

    WriteResponse write(Key key, Value value);

    List<UsageResponse> usageStats();
}
