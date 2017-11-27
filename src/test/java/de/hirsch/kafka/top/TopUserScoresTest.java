package de.hirsch.kafka.top;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
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
		String g1 = "g1";
		driver.process(TopUserScores.USER_SCORE_STREAM_NAME, new UserGame("u1", g1), 10L, userGameSerde.serializer(), longSerde.serializer());
		driver.process(TopUserScores.USER_SCORE_STREAM_NAME, new UserGame("u2", g1), 20L, userGameSerde.serializer(), longSerde.serializer());
		driver.process(TopUserScores.USER_SCORE_STREAM_NAME, new UserGame("u3", g1), 30L, userGameSerde.serializer(), longSerde.serializer());
		driver.process(TopUserScores.USER_SCORE_STREAM_NAME, new UserGame("u4", g1), 40L, userGameSerde.serializer(), longSerde.serializer());
		driver.process(TopUserScores.USER_SCORE_STREAM_NAME, new UserGame("u5", g1), 50L, userGameSerde.serializer(), longSerde.serializer());
		driver.process(TopUserScores.USER_SCORE_STREAM_NAME, new UserGame("u6", g1), 60L, userGameSerde.serializer(), longSerde.serializer());
		driver.process(TopUserScores.USER_SCORE_STREAM_NAME, new UserGame("u7", g1), 70L, userGameSerde.serializer(), longSerde.serializer());
		
		ProducerRecord<String, String> output = driver.readOutput(TopUserScores.TOP_GAME_USERS_STREAM_NAME, stringSerde.deserializer(), stringSerde.deserializer());
		
		Assert.assertEquals(g1, output.key());
		Assert.assertEquals("u7\nu6\nu5\nu4\nu3\n", output.value());
	}
	
	@Test
	public void testUserGameJson() {
		UserGame ug = new UserGame("u1", "g1");
		byte [] bytes = userGameSerde.serializer().serialize(null, ug);
		Object o = userGameSerde.deserializer().deserialize(null, bytes);
		Assert.assertEquals(ug, o);
	}
}
