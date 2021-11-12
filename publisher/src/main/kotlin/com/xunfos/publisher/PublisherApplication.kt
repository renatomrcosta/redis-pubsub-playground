package com.xunfos.publisher

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.supervisorScope
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors
import kotlin.random.Random


@SpringBootApplication
class PublisherApplication

fun main(args: Array<String>) {
    runApplication<PublisherApplication>(*args)
}

data class MyDto(val id: Long, val name: String)

@Configuration
class RedisConfiguration {
    @Bean
    fun channelTopic(): ChannelTopic = ChannelTopic("redisTopic")

    @Bean
    fun reactiveRedisConnectionFactory(): ReactiveRedisConnectionFactory {
        return LettuceConnectionFactory("localhost", 6379)
    }
}

@RestController
@RequestMapping("/producer")
class Startup(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val channelTopic: ChannelTopic,
    private val objectMapper: ObjectMapper,
) {
    private val separateScope = CoroutineScope(
        Executors.newCachedThreadPool().asCoroutineDispatcher()
    )

    private val random = Random(System.currentTimeMillis())

    @PostMapping("/single")
    suspend fun launchSingleMessage() {
        separateScope.launch {
            val id = random.nextLong(0L, 10000L)
            val message = MyDto(
                id = id,
                name = "Single message sent | Message $id"
            )
            withCounter("number_of_messages") {
                redisTemplate.convertAndSend(channelTopic.topic, message.toJson()).awaitSingle()
            }
        }
    }

    @PostMapping
    suspend fun launchMessageProducer() {
        separateScope.launch {
            withCounter("number_of_producers") {}

            var repetitionIndex = 0
            while (this.isActive) {
                val numberOfMessages = random.nextInt(10, 100)
                println("Executing repetition $repetitionIndex with $numberOfMessages messages")

                supervisorScope {
                    repeat(numberOfMessages) { msgIndex ->
                        launch {
                            val message = MyDto(
                                id = msgIndex.toLong(),
                                name = "Repetition $repetitionIndex | Message $msgIndex"
                            )
                            withCounter("number_of_messages") {
                                redisTemplate.convertAndSend(channelTopic.topic, message.toJson()).awaitSingle()
                            }
                        }
                    }
                }

                println("Finished repetition $repetitionIndex with $numberOfMessages messages")
                repetitionIndex++
            }
        }
    }


    private fun MyDto.toJson(): String = objectMapper.writeValueAsString(this)
}


