Use Kafka Streams to process data
Kafka Streams is a client library for building mission-critical real-time applications and microservices, where the input and/or output data is stored in Kafka clusters. Kafka Streams combines the simplicity of writing and deploying standard Java and Scala applications on the client side with the benefits of Kafka's server-side cluster technology to make these applications highly scalable, elastic, fault-tolerant, distributed, and much more. This example will demonstrate how to run a streaming application coded in this library.

Kafka Streams is a client library for building mission-critical real-time applications and microservices, where the input and/or output data is stored in Kafka clusters. Kafka Streams combines the simplicity of writing and deploying standard Java and Scala applications on the client side with the benefits of Kafka's server-side cluster technology to make these applications highly scalable, elastic, fault-tolerant, distributed, and much more.

This example will demonstrate how to run a streaming application coded in this library. Here is the gist of the WordCountDemo example code (converted to use Java 8 lambda expressions for easy reading).

This implements the WordCount algorithm, which computes a word occurrence histogram from the input text. However, unlike other WordCount examples you might have seen before that operate on bounded data, the WordCount demo application behaves slightly differently because it is designed to operate on an infinite, unbounded stream of data. Similar to the bounded variant, it is a stateful algorithm that tracks and updates the counts of words. However, since it must assume potentially unbounded input data, it will periodically output its current state and results while continuing to process more data because it cannot know when it has processed "all" the input data.

As the first step, we will start Kafka (unless you already have it started) and then we will prepare input data to a Kafka topic, which will subsequently be processed by a Kafka Streams application.

Step 1: Prepare input topic and start Kafka producer
Next, we create the input topic named streams-plaintext-input and the output topic named streams-wordcount-output:

>kafka-topics --create --bootstrap-server localhost:9092  --replication-factor 1    --partitions 1 --topic streams-plaintext-input

Created topic "streams-plaintext-input".

Note: we create the output topic with compaction enabled because the output stream is a changelog stream  

kafka-topics --create --bootstrap-server localhost:9092  --replication-factor 1 --partitions 1 --topic streams-wordcount-output  --config cleanup.policy=compact

The created topic can be described with the same kafka-topics tool:
 
kafka-topics --bootstrap-server localhost:9092 --describe

 2. Start the Wordcount Application
The following command starts the WordCount demo application:

Compile the WodCountDemo jaav file

javac -cp D:\kafka_2.11-2.2.1\libs\*  WordCountDemo.java


kafka-run-class WordCountDemo

The demo application will read from the input topic streams-plaintext-input, perform the computations of the WordCount algorithm on each of the read messages, and continuously write its current results to the output topic streams-wordcount-output. Hence there won't be any STDOUT output except log entries as the results are written back into in Kafka.

Now we can start the console producer in a separate terminal to write some input data to this topic:

kafka-console-producer --broker-list localhost:9092 --topic streams-plaintext-input

and inspect the output of the WordCount demo application by reading from its output topic with the console consumer in a separate terminal:


kafka-console-consumer --bootstrap-server localhost:9092 --topic streams-wordcount-output --from-beginning --formatter kafka.tools.DefaultMessageFormatter  --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer

 
Step 3: Process some data
Now let's write some message with the console producer into the input topic streams-plaintext-input by entering a single line of text and then hit <RETURN>. 
This will send a new message to the input topic, where the message key is null and the message value is the string encoded text line that you just entered (in practice, input data for applications will typically be streaming continuously into Kafka, rather than being manually entered as we do in this quickstart):

Thius message will be processed by the Wordcount application and the following output data will be written to the streams-wordcount-output topic and printed by the console consumer: 


 Here, the first column is the Kafka message key in java.lang.String format and represents a word that is being counted, and the second column is the message value in java.lang.Longformat, representing the word's latest count.

 kafka-console-consumer --bootstrap-server localhost:9092 --topic streams-wordcount-output  --from-beginning --formatter kafka.tools.DefaultMessageFormatter --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer

The two diagrams below illustrate what is essentially happening behind the scenes. The first column shows the evolution of the current state of the KTable<String, Long> that is counting word occurrences for count. The second column shows the change records that result from state updates to the KTable and that are being sent to the output Kafka topic streams-wordcount-output.
 

And so on (we skip the illustration of how the third line is being processed). This explains why the output topic has the contents we showed above, because it contains the full record of changes.

Looking beyond the scope of this concrete example, what Kafka Streams is doing here is to leverage the duality between a table and a changelog stream (here: table = the KTable, changelog stream = the downstream KStream): you can publish every change of the table to a stream, and if you consume the entire changelog stream from beginning to end, you can reconstruct the contents of the table.


First the text line "all streams lead to kafka" is being processed. The KTable is being built up as each new word results in a new table entry (highlighted with a green background), and a corresponding change record is sent to the downstream KStream.

When the second text line "hello kafka streams" is processed, we observe, for the first time, that existing entries in the KTable are being updated (here: for the words "kafka" and for "streams"). And again, change records are being sent to the output topic.

And so on (we skip the illustration of how the third line is being processed). This explains why the output topic has the contents we showed above, because it contains the full record of changes.
