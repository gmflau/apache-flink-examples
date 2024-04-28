package com.acmecorp.provided;

import org.json.JSONObject;
import org.json.JSONException;
import org.apache.flink.api.common.functions.MapFunction;

public class TransformFunction implements MapFunction<String, UserClickEvent> {

    @Override
    public UserClickEvent map(String value) throws Exception {
        JSONObject jsonObject = new JSONObject(value);
        String userIp = jsonObject.getString("user.ip");
        long userAccountId = jsonObject.getLong("user.accountId");
        String page = jsonObject.getString("page");
        long hostTimestamp = jsonObject.getLong("host.timestamp");
        String host = jsonObject.getString("host");
        long hostSequence = jsonObject.getLong("host.sequence");

      return new UserClickEvent(userIp, userAccountId, page, hostTimestamp, host, hostSequence);
    }
}


