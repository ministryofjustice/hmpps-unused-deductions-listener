package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.service.EventService
import java.util.concurrent.CompletableFuture

@Service
class DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val eventService: EventService,
) {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun onDomainEvent(
    rawMessage: String,
  ): CompletableFuture<Void> {
    log.info("Received message {}", rawMessage)
    val sqsMessage: SQSMessage = objectMapper.readValue(rawMessage)
    return asCompletableFuture {
      when (sqsMessage.Type) {
        "Notification" -> {
          val (eventType) = objectMapper.readValue<HMPPSDomainEvent>(sqsMessage.Message)
          processMessage(eventType, sqsMessage.Message)
        }
      }
    }
  }

  private fun processMessage(eventType: String, message: String) {
    when (eventType) {
      "release-date-adjustments.adjustment.inserted",
      "release-date-adjustments.adjustment.updated",
      "release-date-adjustments.adjustment.deleted",
      ->
        eventService.handleAdjustmentMessage(objectMapper.readValue(message))
      "prisoner-offender-search.prisoner.updated",
      ->
        eventService.handlePrisonerSearchEvent(objectMapper.readValue(message))

      else -> log.info("Received a message I wasn't expecting: {}", eventType)
    }
  }
}

private fun asCompletableFuture(
  process: suspend () -> Unit,
): CompletableFuture<Void> {
  return CoroutineScope(Dispatchers.Default).future {
    process()
  }.thenAccept { }
}
