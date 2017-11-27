package de.hirsch.kafka.top;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameScore {
	public final String name;
	public final long score;
	
	@JsonCreator
	GameScore(@JsonProperty("name") String name, @JsonProperty("score") long score) {
		this.name = name;
		this.score = score;
	}
	
	public String toString() {
		return "GameScore{name="+name+", score="+score+"}";
	}
}
