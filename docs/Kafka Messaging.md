1. open the cmd/terminal in D:\kafka_2.11-2.2.1\bin\windows or D:\kafka_2.11-2.2.1\bin\ directory as per the OS platform.

 Start the ZooKeeper for configuration and management of kafka nodes
 zookeeper-server-start ../../config/zookeeper.properties
 OR
zookeeper-server-start ../../config/zookeeper.properties

 2.After starting the zookeeper, Start the kafka Broker from another terminal in same path as above

   kafka-server-start ../../config/server.properties

   For windows set the path as belowill
   set path=%path%;C:\Windows\winsxs\x86_microsoft-windows-w..ommand-line-utility_31bf3856ad364e35_6.1.7600.16385_none_a1802b822e2a878c;


  OR
 kafka-server-start.sh ../../config/server.properties


3. create a topic named "test" with a single partition and only one replica

  kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic test

   kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic test

  now see that topic  by running the list topic command:

  kafka-topics --list --bootstrap-server localhost:9092

   Instead of manually creating topics you can also configure your brokers to auto-create topics when a non-existent topic is published to.

config/server.properties file. This file is just key value property file. One of the properties is auto.create.topics.enable, if it's set to true (by default)

 allow.auto.create.topics=true  in config/server.proiperties file.

  4.Run command line client that will take input from a file or from standard input and send it out as messages to the Kafka cluster. By default, each line will be sent as a separate message.

Run the producer and then type a few messages into the console to send to the server.3
> bin/windows/kafka-console-producer --broker-list localhost:9092 --topic test
HI
Hello
Dada

  5. Read the message from command line consumer that will dump out messages to standard output.

kafka-console-consumer --bootstrap-server localhost:9092 --topic test --from-beginning

  Running the producer and consusmer commands from different terminal ,now be able to type messages into the producer terminal and see them appear in the consumer terminal.

 

6: Setting up a multi-broker cluster
Earlier running against a single broker.
For Kafka, a single broker is just a cluster of size one, so nothing much changes other than starting a few more broker instances. let's expand our cluster to three nodes (still all on same local machine).

First make a config file for each of the brokers (on Windows use the copy command instead):

copy ../../config/server.properties ../../config/server-1.properties
copy ../../config/server.properties ../../config/server-2.properties
 
 On Linux 
> cp config/server.properties config/server-1.properties
> cp config/server.properties config/server-2.properties

Edit these new files and set the following properties:

 
config/server-1.properties:
    broker.id=1
    listeners=PLAINTEXT://:9093
    log.dirs=/tmp/kafka-logs-1
 
config/server-2.properties:
    broker.id=2
    listeners=PLAINTEXT://:9094
    log.dirs=/tmp/kafka-logs-2

  The broker.id property is the unique and permanent name of each node in the cluster. Need to override the port and log directory only because we are running these all on the same machine and we have  to keep the brokers from all trying to register on the same port or overwrite each other's data.

  to start the two new nodes:from kafka/bin/windows dir in new terminal
 Start  kafka-server-start.bat ../../config/server-1.properties  (To run in background mode)

   
 Start  kafka-server-start.bat ../../config/server-2.properties 

   To redirect  the log output to file
 

   redirect the output to a file:

 Start  kafka-server-start.bat ../../config/server-1.properties  > output.txt
   
  On Linux

  kafka-server-start  ../../config/server-1.properties &

  kafka-server-start  ../../config/server-2.properties &



  7. Create a new topic with a replication factor of three or two depending on number of kafka nodes started:

kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 2 --partitions 1 --topic my-replicated-topic

  To find out which broker is doing what? To see that run the "describe topics" command:

kafka-topics --describe --bootstrap-server localhost:9092 --topic my-replicated-topic


Topic:my-replicated-topic       PartitionCount:1        ReplicationFactor:2     Configs:segment.bytes=1073741824
        Topic: my-replicated-topic      Partition: 0    Leader: 1       Replicas: 1,0   Isr: 1,0

  The first line gives a summary of all the partitions, each additional line gives information about one partition. Since we have only one partition for this topic there is only one line.

"leader" is the node responsible for all reads and writes for the given partition. Each node will be the leader for a randomly selected portion of the partitions.
"replicas" is the list of nodes that replicate the log for this partition regardless of whether they are the leader or even if they are currently alive.
"isr" is the set of "in-sync" replicas. This is the subset of the replicas list that is currently alive and caught-up to the leader.

 To find out topic test
 kafka-topics --describe --bootstrap-server localhost:9092 --topic test

  8.  publish a few messages to new topic:
 kafka-console-producer --broker-list localhost:9092 --topic my-replicated-topic

  consume these messages:
kafka-console-consumer --bootstrap-server localhost:9092 --from-beginning --topic my-replicated-topic
 
To test out fault-tolerance. Broker 1 was acting as the leader so let's kill it:

On Windows use:
wmic process where "caption = 'java.exe' and commandline like '%server-1.properties%'" get processid

ProcessId 
7188
> taskkill /pid 7188 /f

 On Linux
 : ps aux | grep server-1.properties
  kill -9 7564

  Leadership has switched to one of the followers and node 1 is no longer in the in-sync replica set:
kafka-topics --describe --bootstrap-server localhost:9092 --topic my-replicated-topic

But the messages are still available for consumption even though the leader that took the writes originally is down:

kafka-console-consumer --bootstrap-server localhost:9092 --from-beginning --topic my-replicated-topic
 

 7: Use Kafka Connect to import/export data
 
Writing data from the console and writing it back to the console is a convenient place to start, but you'll probably want to use data from other sources or export data from Kafka to other systems. For many systems, instead of writing custom integration code you can use Kafka Connect to import or export data.

Kafka Connect is a tool included with Kafka that imports and exports data to Kafka. It is an extensible tool that runs connectors, which implement the custom logic for interacting with an external system. In this quickstart we'll see how to run Kafka Connect with simple connectors that import data from a file to a Kafka topic and export data from a Kafka topic to a file.

First, we'll start by creating some seed data to test with:

 echo -e "Hello\nBye!" > test.txt
 echo -e "welcome\nUser!" > test.txt

  on Windows

  echo  "Hello\nBye!"> test.txt
 echo  "welcome\nUser!">> test.txt

  Now start two connectors running in standalone mode, which means they run in a single, local, dedicated process. We provide three configuration files as parameters. The first is always the configuration for the Kafka Connect process, containing common configuration such as the Kafka brokers to connect to and the serialization format for data. The remaining configuration files each specify a connector to create. These files include a unique connector name, the connector class to instantiate, and any other configuration required by the connector.

connect-standalone ../../config/connect-standalone.properties ../../config/connect-file-source.properties ../../config/connect-file-sink.properties

number of log messages, including some indicating that the connectors are being instantiated. Once the Kafka Connect process has started, the source connector should start reading lines from test.txt and producing them to the topic connect-test, and the sink connector should start reading messages from the topic connect-test and write them to the file test.sink.txt. We can verify the data has been delivered through the entire pipeline by examining the contents of the output file:


more test.sink.txt

the data is being stored in the Kafka topic connect-test, so we can also run a console consumer to see the data in the topic (or use custom consumer code to process it):

kafka-console-consumer --bootstrap-server localhost:9092 --topic connect-test --from-beginning

The connectors continue to process data, so we can add data to the file and see it move through the pipeline:

echo Another line>> test.txt
see the line appear in the console consumer output and in the sink file also.
 



