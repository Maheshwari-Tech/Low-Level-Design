package com.mycompany.app;

import com.mycompany.app.models.ReadResponse;
import com.mycompany.app.models.UsageResponse;
import com.mycompany.app.models.WriteResponse;

import java.util.ArrayList;
import java.util.List;

public class NullEffectLevelCache<Key, Value> implements ILevelCache<Key, Value>{

    @Override
    public ReadResponse<Value> read(Key key) {
        return new ReadResponse<>(null,0.0);
    }

    @Override
    public WriteResponse write(Key key, Value value) {
        return new WriteResponse(0.0);
    }

    @Override
    public List<UsageResponse> usageStats() {
        return new ArrayList<>();
    }
}
