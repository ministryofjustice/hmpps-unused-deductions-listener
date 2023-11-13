package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.listener

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration.wiremock.AdjustmentsApiExtension
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

const val ADJUSTMENT_ID = "a4cef857-cb1e-43e4-b905-13e85fee7538"
const val OFFENDER_NUMBER = "A1234TT"
const val BOOKING_ID = 987651L

class DomainEventListenerIntTest : SqsIntegrationTestBase() {

  @Test
  fun handleAdjustmentEvent() {
    val eventType = "release-date-adjustments.adjustment.inserted"
    awsSnsClient.publish(
      PublishRequest.builder().topicArn(topicArn)
        .message(sentencingAdjustmentMessagePayload(ADJUSTMENT_ID, OFFENDER_NUMBER, eventType, "DPS"))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(eventType).build(),
          ),
        ).build(),
    ).get()

    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(1)
    }
    await untilAsserted {
      assertThat(awsSqsUnusedDeductionsClient!!.countAllMessagesOnQueue(unusedDeductionsQueueUrl).get()).isEqualTo(0)
    }

    await untilAsserted {
      AdjustmentsApiExtension.adjustmentsApi.verify(
        WireMock.postRequestedFor(WireMock.urlEqualTo("/adjustments"))
          .withRequestBody(WireMock.matchingJsonPath("days", WireMock.equalTo("10"))),
      )
      AdjustmentsApiExtension.adjustmentsApi.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/adjustments/$ADJUSTMENT_ID/effective-days")))
    }
  }

  fun sentencingAdjustmentMessagePayload(adjustmentId: String, nomsNumber: String, eventType: String, source: String = "DPS") =
    """{"eventType":"$eventType", "additionalInformation": {"id":"$adjustmentId", "offenderNo": "$nomsNumber", "source": "$source"}}"""
}
