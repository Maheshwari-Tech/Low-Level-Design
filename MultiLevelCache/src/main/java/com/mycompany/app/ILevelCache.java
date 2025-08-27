package com.mycompany.app;

import com.mycompany.app.models.ReadResponse;
import com.mycompany.app.models.UsageResponse;
import com.mycompany.app.models.WriteResponse;

import java.util.List;

public interface ILevelCache<Key, Value> {

    ReadResponse<Value> read(Key key);

    WriteResponse write(Key key, Value value);

    List<UsageResponse> usageStats();
}
