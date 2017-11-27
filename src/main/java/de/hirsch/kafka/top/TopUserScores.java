package de.hirsch.kafka.top;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Properties;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.TimeWindowedKStream;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.kstream.internals.WindowedDeserializer;
import org.apache.kafka.streams.kstream.internals.WindowedSerializer;
import org.apache.kafka.streams.state.WindowStore;

import com.fasterxml.jackson.databind.type.TypeFactory;

import de.hirsch.kafka.JsonSerde; 

/** 
  * Create a data feed of the top N users per game, ranked by sum of scores per hour
	* Based on TopArticlesLambdaExample.java
  * 
  */ 
public class TopUserScores { 

	public static final String USER_SCORE_STREAM_NAME = "user-scores";
	public static final String TOP_GAME_USERS_STREAM_NAME = "top-game-users";

	public static Topology createTopology() {

		StreamsBuilder builder = new StreamsBuilder(); 
		TypeFactory jsonTypeFactory = TypeFactory.defaultInstance();
		final Serde<UserGame> userGameSerde = new JsonSerde<UserGame>(jsonTypeFactory.constructSimpleType(UserGame.class, null)); 
		final Serde<Long> longSerde = Serdes.Long(); 
		KStream<UserGame, Long> userScores = builder.stream(USER_SCORE_STREAM_NAME, 
				Consumed.with(userGameSerde, longSerde)); 

		KGroupedStream<UserGame, Long> groupedUserScores = 
				userScores.groupByKey(Serialized.with(userGameSerde, longSerde));
		// count the scores per hour, using tumbling windows with a size of one hour
		TimeWindowedKStream<UserGame, Long> hourGroupedUserScores = groupedUserScores.windowedBy(TimeWindows.of(60 * 60 * 1000L));
		KTable<Windowed<UserGame>, Long> aggregatedUserScores = hourGroupedUserScores.aggregate(
				() -> 0L, 
				(userGame, score, scoreSum) -> scoreSum + score,
				Materialized.<UserGame, Long, WindowStore<Bytes,byte[]>>with(userGameSerde, longSerde));

		// key by game
		final Serde<GameScore> gameScoreSerde = 
				new JsonSerde<GameScore>(jsonTypeFactory.constructSimpleType(GameScore.class, null)); 
		final Serde<String> stringSerde = Serdes.String(); 
		final Serde<Windowed<String>> windowedStringSerde = Serdes.serdeFrom(new WindowedSerializer<>(stringSerde.serializer()), 
				new WindowedDeserializer<>(stringSerde.deserializer())); 
		final Serde<PriorityQueue<GameScore>> userScoreQueueSerde = 
				new JsonSerde<PriorityQueue<GameScore>>(jsonTypeFactory.constructParametricType(PriorityQueue.class, GameScore.class)); 
		KTable<Windowed<String>, PriorityQueue<GameScore>> allUserScores = aggregatedUserScores 
				.groupBy( 
						// the selector 
						(windowedUserGame, score) -> { 
							// project on the game for key 
							Windowed<String> windowedGame = 
									new Windowed<>(windowedUserGame.key().game, windowedUserGame.window()); 
							// add the user and score into the value 
							GameScore value = new GameScore(windowedUserGame.key().user, score); 
							return new KeyValue<>(windowedGame, value); 
						}, 
						Serialized.with(windowedStringSerde, gameScoreSerde) 
						).aggregate( 
								// the initializer 
								() -> { 
									Comparator<GameScore> comparator = 
											((o1, o2) -> {
												long diff = o1.score - o2.score;
												return diff < 0 ? -1 : (diff == 0 ? 0 : +1);
											});
											return new PriorityQueue<>(comparator); 
								}, 


								// the "add" aggregator 
								(windowedGame, record, queue) -> { 
									queue.add(record); 
									return queue; 
								}, 

								// the "remove" aggregator 
								(windowedGame, record, queue) -> { 
									queue.remove(record); 
									return queue; 
								}, 
								Materialized.as("AllUserScores")/*.withValueSerde(userScoreQueueSerde)*/ 
								); 

		int topN = 5; 
		KTable<Windowed<String>, String> topGameUsers = allUserScores 
				.mapValues(queue -> { 
					StringBuilder sb = new StringBuilder(); 
					for (int i = 0; i < topN; i++) { 
						GameScore record = queue.poll(); 
						if (record == null) 
							break; 
						sb.append(record.name); 
						sb.append("\n"); 
					}
					return sb.toString(); 
				}); 

		topGameUsers.toStream().to(TOP_GAME_USERS_STREAM_NAME, Produced.with(windowedStringSerde, stringSerde)); 
		return builder.build();
	}
	
	public static Properties createConfiguration() {
		Properties streamsConfiguration = new Properties(); 
		// Give the Streams application a unique name.  The name must be unique in the Kafka cluster 
		// against which the application is run. 
		streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "top-game-user-scores"); 
		// Where to find Kafka broker(s). 
		streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"); 
		// Specify default (de)serializers for record keys and for record values. 
		final Serde<String> stringSerde = Serdes.String(); 
		streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, stringSerde.getClass().getName()); 
		streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, stringSerde.getClass().getName());
		return streamsConfiguration;
	}
	
	public static void main(String[] args) throws Exception { 

		Topology topology = createTopology();
		Properties streamsConfiguration = createConfiguration(); 
		KafkaStreams streams = new KafkaStreams(topology, streamsConfiguration); 
		streams.start(); 
	} 
} 
