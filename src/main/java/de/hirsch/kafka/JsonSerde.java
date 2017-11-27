package de.hirsch.kafka;

import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JsonSerde<T> implements Serde<T>{

	private static class JsonSerializer<T> implements Serializer<T> {

		private final ObjectWriter objectWriter;
		
		private JsonSerializer(ObjectMapper objectMapper, JavaType javaType) {
	    objectWriter = objectMapper.writerFor(javaType);			
		}
		
		@Override
		public void configure(Map<String, ?> configs, boolean isKey) {
		}

		@Override
		public byte[] serialize(String topic, T data) {
	    if (data == null)
	      return null;
	    try {
				return objectWriter.writeValueAsBytes(data);
			} catch (JsonProcessingException e) {
	      throw new SerializationException("Error serializing "+data, e);
			}
		}

		@Override
		public void close() {
		}		
	}
	
	private static class JsonDeserializer<T> implements Deserializer<T> {

		private final ObjectReader objectReader;
		
		private JsonDeserializer(ObjectMapper objectMapper, JavaType javaType) {
	    objectReader = objectMapper.readerFor(javaType);			
		}

		@Override
		public void configure(Map<String, ?> configs, boolean isKey) {
		}

		@Override
		public T deserialize(String topic, byte[] bytes) {
	    if (bytes == null)
	      return null;
			try {
				return objectReader.readValue(bytes, 0, bytes.length);
			} catch (IOException e) {
	      throw new SerializationException(e);
			}
		}

		@Override
		public void close() {
		}
	}

	private final JsonSerializer<T> jsonSerializer;
	private final JsonDeserializer<T> jsonDeserializer;
	
	/**
	 * The JavaType argument must describe template type T;
	 * this is needed because of type erasure.
	 * @param javaType
	 */
	public JsonSerde(JavaType javaType) {
		ObjectMapper objectMapper = new ObjectMapper();
		jsonSerializer = new JsonSerializer<T>(objectMapper, javaType);
		jsonDeserializer = new JsonDeserializer<T>(objectMapper, javaType);
	}
	
	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
	}

	@Override
	public void close() {
	}

	@Override
	public Serializer<T> serializer() {
		return jsonSerializer;
	}

	@Override
	public Deserializer<T> deserializer() {
		return jsonDeserializer;
	}

}
