# Redis Pub/Sub study Playground

Heard about [Redis Pub/Sub](https://redis.io/topics/pubsub). Quoting the documentation:

> SUBSCRIBE, UNSUBSCRIBE and PUBLISH implement the Publish/Subscribe messaging paradigm where (citing Wikipedia) senders (publishers) are not programmed to send their messages to specific receivers (subscribers). Rather, published messages are characterized into channels, without knowledge of what (if any) subscribers there may be. Subscribers express interest in one or more channels, and only receive messages that are of interest, without knowledge of what (if any) publishers there are. This decoupling of publishers and subscribers can allow for greater scalability and a more dynamic network topology.

This repo adds two Spring applications (one consumer and a publisher), running on ports 8080 and 9090 respectively, and a docker-compose to start a local redis and redis-insights container in their default local ports.

The consumer and publisher use the artifice of registering consumers / publishers using HTTP requests, that can be accessed and mapped by the [requests.http](/requests.http) request file.

### Current conclusions

- Dead simple to connect to and play around with;
- Interesting way to get different services to consume a central state with Channels together;
- As usual, it's no silver bullet: within service Boundaries a simple coroutine Channel solves the issue ([see a Producer pattern here](https://github.com/renatomrcosta/KtPlayground/blob/master/src/main/kotlin/coroutines/channels/6_fan_out.kt)), for more complex use cases one can start considering fully fledged [Message](https://zeromq.org/) [Queues](https://www.rabbitmq.com/) or [Event Streaming platforms](https://kafka.apache.org/).
- Works with ElasticCache! Getting this stuff clusterized in an infrastructure is a terraform file away!
