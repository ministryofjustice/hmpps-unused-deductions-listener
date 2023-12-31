package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.Adjustment
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentEffectiveDays
import java.util.UUID

@Service
class AdjustmentsApiClient(@Qualifier("adjustmentsApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getAdjustmentsByPerson(prisonerId: String): List<Adjustment> {
    log.info("Getting adjustment details for prisoner $prisonerId")
    return webClient.get()
      .uri("/adjustments?person=$prisonerId")
      .retrieve()
      .bodyToMono(typeReference<List<Adjustment>>())
      .block()!!
  }

  fun updateEffectiveDays(effectiveDays: AdjustmentEffectiveDays) {
    log.info("Updating effective days details for prisoner ${effectiveDays.person}")
    webClient.post()
      .uri("/adjustments/{adjustmentId}/effective-days", effectiveDays.id)
      .bodyValue(effectiveDays)
      .retrieve()
      .toBodilessEntity()
      .block()!!
  }

  fun updateAdjustment(adjustment: Adjustment) {
    log.info("Updating adjustment details for prisoner ${adjustment.person}")
    webClient.put()
      .uri("/adjustments/{adjustmentId}", adjustment.id)
      .bodyValue(adjustment)
      .retrieve()
      .toBodilessEntity()
      .block()!!
  }

  fun createAdjustment(adjustment: Adjustment) {
    log.info("Create adjustment details for prisoner ${adjustment.person}")
    webClient.post()
      .uri("/adjustments")
      .bodyValue(listOf(adjustment))
      .retrieve()
      .toBodilessEntity()
      .block()!!
  }

  fun deleteAdjustment(id: UUID) {
    log.info("Delete adjustment details")
    webClient.delete()
      .uri("/adjustments/{adjustmentId}", id)
      .retrieve()
      .toBodilessEntity()
      .block()!!
  }
}
