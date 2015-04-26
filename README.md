# java-stream-benchmarks
Micro benchmarks comparing various stream and non stream implementations of a summing algorithm. The benchmark was
performed with the [http://java-performance.info/jmh/](JMH) benchmark. 
See this [https://www.tobyhobson.co.uk/java-8-streams-performance/](blog post) for more information

To run the benchmarks execute `mvn clean install && java -server -jar target/benchmarks.jar`

To ensure the benchmarks work correctly add the assertions flag `java -server -ea -jar target/benchmarks.jar`

The stream based implementation are really trivial but they demonstrate the importance of choosing 
the correct collection implementation. The "traditional" algorithms are more complex but in testing proved to be
much faster. For example the standard stream using a LinkedList gave 168 ops/s on my machine whereas an optimised
multi threaded for loop gave over 10,000 ops/s. 


