package de.hirsch.kafka.top;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserGame {

	public final String user;
	public final String game;
	
	@JsonCreator
	public UserGame(@JsonProperty("user") String user, @JsonProperty("game") String game) {
		this.user = user;
		this.game = game;
	}
	
	public boolean equals(Object o) {
		if (o instanceof UserGame) {
			UserGame other = (UserGame) o;
			return user.equals(other.user) && game.equals(other.game);
		}
		return false;
	}
}
