package com.java;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

 
public class ConsumerWithCommitOffset implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ConsumerWithCommitOffset.class);
	
	private String topic;
	
	public ConsumerWithCommitOffset(String topic) {
		this.topic = topic;
	}

	public static void main(String args[])
	{
	 System.out.println("Starting ConsumerWithCommitOffset...");
	 ConsumerWithCommitOffset producer = new ConsumerWithCommitOffset("testTopic");
	 Thread runner = new Thread(producer);
	 runner.start();
	}
	
	@Override
	public void run() {
		//Add offset 
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("group.id", "test1");
		props.put("enable.auto.commit", "false");
		props.put("auto.commit.interval.ms", "1000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
		
		TopicPartition partition0 = new TopicPartition("testTopic", 0); //second param is partitioon number
		//consumer.subscribe(Arrays.asList("testTopic"));
		consumer.assign(Arrays.asList(partition0));
		
		try {
	         while(true) {
	             ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
	             for (TopicPartition partition : records.partitions()) {
	                 List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
	                 for (ConsumerRecord<String, String> record : partitionRecords) {
	                     System.out.println(record.offset() + ": " + record.value());
	                 }
	                 long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
	                 System.out.println("Commited offset by consumer = "+lastOffset);
	                 consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
	             }
	         }
	     } finally {
	       consumer.close();
	     }		 
		 
		
	}
}
