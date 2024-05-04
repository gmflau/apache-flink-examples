package com.acmecorp;

import com.acmecorp.provided.UserClickEvent;
import com.acmecorp.provided.TransformFunction;
import com.acmecorp.provided.ClickEventGenerator;
import com.acmecorp.provided.CountUserEvents;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.watermark.Watermark; 
import org.apache.flink.streaming.api.operators.AbstractStreamOperator; 
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.api.java.tuple.Tuple2;
import java.time.Duration;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.windowing.triggers.EventTimeTrigger;


public class StreamingJob {

	public static void main(String[] args) throws Exception {
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		final ParameterTool pt = ParameterTool.fromArgs(args);

		System.out.println("Reached StreamingJob");

		//env.enableCheckpointing(10000L);
		DataStream<String> input = env.addSource(new ClickEventGenerator(pt));

		// Define a function to transform string to object
		MapFunction<String, UserClickEvent> transformFunction = new TransformFunction();

		// Apply the transformation and print the results
		DataStream<UserClickEvent> objectStream = input.map(transformFunction);
		//objectStream.print();

		// Use watermark strategy to leverage a timestamp field of events
		WatermarkStrategy<UserClickEvent> watermarkStrategy = WatermarkStrategy
			.<UserClickEvent>forBoundedOutOfOrderness(Duration.ofSeconds(5))
			.withTimestampAssigner((event, timestamp) -> event.hostTimestamp);
		DataStream<UserClickEvent> wm_objectstream = objectStream.assignTimestampsAndWatermarks(watermarkStrategy);

		// Remove all unauthenticated visitor actions
		DataStream<UserClickEvent> filtered_wm_objectstream = wm_objectstream.filter(value -> value.userAccountId != -1);

		DataStream<Tuple2<Long, Long>> userEventCounts = filtered_wm_objectstream
			.keyBy(event -> event.userAccountId)
			.window(SlidingEventTimeWindows.of(Time.minutes(10), Time.minutes(1)))
			.aggregate(new CountUserEvents());

		//userEventCounts.print();

		// Create one stream for the number of user clicks is 10 or more
		// Create another stream for the number of user clicks is less than 10
		DataStream<Tuple2<Long, Long>> userEventCountsGreaterThanNine = userEventCounts.filter(value -> value.f1 > 9);
		DataStream<Tuple2<Long, Long>> userEventCountsLessThanTen = userEventCounts.filter(value -> value.f1 < 10);

		// Saving user click events on files
		String outputPathForTenPlus = "/Users/glau/Documents/Flink/UserEvents/output/userevents_10plus";
		final FileSink<Tuple2<Long, Long>> sinkTenPlus = FileSink
				.forRowFormat(new Path(outputPathForTenPlus),
                	new SimpleStringEncoder < Tuple2 < Long, Long >> ("UTF-8"))
                .withRollingPolicy(DefaultRollingPolicy.builder()
					.withRolloverInterval(Duration.ofMinutes(10))
					.withInactivityInterval(Duration.ofMinutes(5))
					.withMaxPartSize(MemorySize.ofMebiBytes(1024))
					.build())
                .build();
		userEventCountsGreaterThanNine.sinkTo(sinkTenPlus);
		
		String outputPathForLessThanTen = "/Users/glau/Documents/Flink/UserEvents/output/userevents_less_than_10";
		final FileSink<Tuple2<Long, Long>> sinkLessThanTen = FileSink
				.forRowFormat(new Path(outputPathForLessThanTen),
                	new SimpleStringEncoder < Tuple2 < Long, Long >> ("UTF-8"))
                .withRollingPolicy(DefaultRollingPolicy.builder()
					.withRolloverInterval(Duration.ofMinutes(10))
					.withInactivityInterval(Duration.ofMinutes(5))
					.withMaxPartSize(MemorySize.ofMebiBytes(1024))
					.build())
                .build();
		userEventCountsLessThanTen.sinkTo(sinkLessThanTen);
		
		env.execute("User Click Events");
		System.out.println("Leaving StreamingJob");
	}

}
