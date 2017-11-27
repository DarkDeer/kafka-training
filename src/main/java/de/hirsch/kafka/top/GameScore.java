package de.hirsch.kafka.top;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GameScore implements Comparable<GameScore> {
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

	@Override
	public int compareTo(GameScore o) {
		int cmp = Long.compare(score, o.score);
		// largest score 1st (so as to get the top N as the head of the PriorityQueue)
		return cmp == 0 ? name.compareTo(o.name) : -cmp;
	}
}
