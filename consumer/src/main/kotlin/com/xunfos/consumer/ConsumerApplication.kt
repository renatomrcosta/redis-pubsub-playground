package com.xunfos.consumer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.listenToAsFlow
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.Executors

@SpringBootApplication
class ConsumerApplication

fun main(args: Array<String>) {
    runApplication<ConsumerApplication>(*args)
}

@Configuration
class RedisConfiguration() {
    @Bean
    fun channelTopic(): ChannelTopic = ChannelTopic("redisTopic")

    @Bean
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        return LettuceConnectionFactory("localhost", 6379)
    }
}

@RestController
@RequestMapping("/consumer")
class RequesterSetup(
    private val channelTopic: ChannelTopic,
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
) {
    private val separateScope = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())

    @GetMapping
    suspend fun subscribe() {
        separateScope.launch {
            withCounter("number_of_consumers") {}

            val id = UUID.randomUUID().toString()
            println("Launching listener for Topic with ID $id")
            redisTemplate.listenToAsFlow(channelTopic)
                .collect { message ->
                    withCounter("number_of_messages_consumed", listOf("consumer", id)) {
                        println("📬 $id 📬 | ${message.message}")
                    }
                }
        }
    }

    @GetMapping("/slow")
    suspend fun subscribeWithSlowOp(): Unit = withCounter("number_of_consumers") {
        separateScope.launch {
            val id = UUID.randomUUID().toString()
            println("Launching listener that does some heavy work for Topic with ID $id")
            redisTemplate
                .listenToAsFlow(channelTopic)
                .buffer(10)
                .collect { message ->
                    withCounter("number_of_messages_consumed", listOf("consumer", id)) {
                        println("🐌 ${message.message} | Starting some really slow job 🐌")
                        delay(1_000)
                        println("🐌 ${message.message} | Finished with that slow job 🐌")
                    }
                }
        }
    }
    @GetMapping("/latest")
    suspend fun subscribeWithConflation(): Unit = withCounter("number_of_consumers") {
        separateScope.launch {
            val id = UUID.randomUUID().toString()
            println("Launching listener that does some heavy work for Topic with ID $id, but only cares about most recent results")
            redisTemplate
                .listenToAsFlow(channelTopic)
                .conflate()
                .buffer(10)
                .collect { message ->
                    withCounter("number_of_messages_consumed", listOf("consumer", id)) {
                        println("🐌 ${message.message} | Starting some really slow job 🐌")
                        delay(1_000)
                        println("🐌 ${message.message} | Finished with that slow job 🐌")
                    }
                }
        }
    }
}
