package com.java;

import java.util.Properties;
import java.util.Scanner;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 
public class Producer implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(Producer.class);
	
	private String message;
	private String topic;
	
	public static void main(String args[])
	{
	 System.out.println("Starting Producer...");
	 Producer producer = new Producer("testTopic","THis is my message");
	 Thread runner = new Thread(producer);
	 runner.start();
	}
	
	public Producer(String topic, String message) {
		this.message = message;
		this.topic = topic;
	}

	@Override
	public void run() {
		
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		KafkaProducer<String, String> producer = new KafkaProducer<>(props);
		
		Scanner sc = new Scanner(System.in);
		
		System.out.println("Enter your message to send ");
		
	    String message = sc.nextLine();
		
		producer.send(new ProducerRecord<String, String>(this.topic, message));
		
		logger.info("Message sent to topic: {}", this.topic);
		
		System.out.println("Message sent to topic: {}"+ this.topic + "  "+ message);
		
		producer.close();
	}
}
