package com.acmecorp.provided;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;

// Implement an AggregateFunction to count visits
public class CountUserEvents implements AggregateFunction<UserClickEvent, EventAccumulator, Tuple2<Long, Long>> {

public EventAccumulator createAccumulator() {
    return new EventAccumulator();
}

public EventAccumulator add(UserClickEvent event, EventAccumulator accumulator) {
    accumulator.userAccountId = event.userAccountId;
    accumulator.count += 1;
    return accumulator;
}

//public Tuple2<Long, Long> getResult(EventAccumulator accumulator) {
//	return new Tuple2<Long, Long>(accumulator.userAccountId, accumulator.count);
//}

public Tuple2<Long, Long> getResult(EventAccumulator accumulator) {

    return new Tuple2(accumulator.userAccountId, accumulator.count);
}

public EventAccumulator merge(EventAccumulator acc1, EventAccumulator acc2) {
    acc1.count += acc2.count;
    return acc1;
}
}
