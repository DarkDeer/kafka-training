package de.hirsch.kafka.top;

import java.text.SimpleDateFormat;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.internals.WindowedDeserializer;
import org.apache.kafka.streams.kstream.internals.WindowedSerializer;
import org.apache.kafka.test.ProcessorTopologyTestDriver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.type.TypeFactory;

import de.hirsch.kafka.JsonSerde;

public class TopUserScoresTest {

	private final Serde<UserGame> userGameSerde = new JsonSerde<UserGame>(TypeFactory.defaultInstance().constructSimpleType(UserGame.class, null));
	private final Serde<Long> longSerde = Serdes.Long();
	private final Serde<String> stringSerde = Serdes.String();
	private ProcessorTopologyTestDriver driver;
	
	@Before
	public void before() {
		Topology topology = TopUserScores.createTopology();
		System.out.println("topology description = "+topology.describe());
		StreamsConfig streamsConfig = new StreamsConfig(TopUserScores.createConfiguration());
		driver = new ProcessorTopologyTestDriver(streamsConfig, topology);
	}
	
	@Test
	public void test() {
		final Serde<Windowed<String>> windowedStringSerde = Serdes.serdeFrom(new WindowedSerializer<>(stringSerde.serializer()), 
				new WindowedDeserializer<>(stringSerde.deserializer())); 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
		String g1 = "g1";
		final int n = 120;
		long now = System.currentTimeMillis();
		now = now - (now%(TopUserScores.TIME_WINDOW_SECONDS*1000L));
		for (int i = 0; i < n; i++) {
			
			driver.process(TopUserScores.USER_SCORE_STREAM_NAME, new UserGame("u"+i, g1), 10L*i+1, userGameSerde.serializer(), longSerde.serializer(), now+(i*1000));

			ProducerRecord<Windowed<String>, String> output = driver.readOutput(TopUserScores.TOP_GAME_USERS_STREAM_NAME, windowedStringSerde.deserializer(), stringSerde.deserializer());

			Assert.assertEquals(g1, output.key().key());
			StringBuilder sb = new StringBuilder();
			for (int j = i; j >= Math.max(i - i%TopUserScores.TIME_WINDOW_SECONDS, i - 4); j--) {
				sb.append("u").append(j).append('\n');
			}
			System.out.println(i+" window: "+sdf.format(output.key().window().start())+","+sdf.format(output.key().window().end()));
			System.out.println(i+" : "+output.value());
			Assert.assertEquals(" window "+i+" "+sdf.format(output.key().window().start()), now + (i/TopUserScores.TIME_WINDOW_SECONDS)*TopUserScores.TIME_WINDOW_SECONDS*1000L, output.key().window().start());
			Assert.assertEquals("Output "+i, sb.toString(), output.value());
			
		}
	}
}
