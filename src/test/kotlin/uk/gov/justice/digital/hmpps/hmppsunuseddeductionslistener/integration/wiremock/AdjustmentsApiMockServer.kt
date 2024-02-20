package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.listener.OFFENDER_NUMBER
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.listener.REMAND_ID
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.listener.TAGGED_BAIL_ID

/*
    This class mocks the adjustments api.
 */
class AdjustmentsApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val adjustmentsApi = AdjustmentsApiMockServer()
  }
  override fun beforeAll(context: ExtensionContext) {
    adjustmentsApi.start()
    adjustmentsApi.stubCreate()
    adjustmentsApi.stubGet()
    adjustmentsApi.stubEffectiveDaysForTaggedBailAdjustment()
  }

  override fun beforeEach(context: ExtensionContext) {
    adjustmentsApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    adjustmentsApi.stop()
  }
}

class AdjustmentsApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8334
  }

  fun stubGet() {
    stubFor(
      get("/adjustments?person=$OFFENDER_NUMBER")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
             [
   {
      "id":"$REMAND_ID",
      "bookingId":1204935,
      "sentenceSequence":1,
      "person":"A1032DZ",
      "adjustmentType":"REMAND",
      "toDate":"2023-01-20",
      "fromDate":"2023-01-10",
      "additionalDaysAwarded":null,
      "unlawfullyAtLarge":null,
      "prisonId":"KMI",
      "prisonName":null,
      "lastUpdatedBy":"NOMIS",
      "status":"ACTIVE",
      "lastUpdatedDate":"2023-10-26T09:57:36.319803",
      "daysTotal":11,
      "effectiveDays":11,
      "remand": {
        "chargeId": [
           3933870
        ]
      }
   },
   {
      "id":"$TAGGED_BAIL_ID",
      "bookingId":1204935,
      "sentenceSequence":1,
      "person":"A1032DZ",
      "adjustmentType":"TAGGED_BAIL",
      "toDate":null,
      "fromDate":"2023-01-10",
      "additionalDaysAwarded":null,
      "unlawfullyAtLarge":null,
      "prisonId":"KMI",
      "prisonName":null,
      "lastUpdatedBy":"NOMIS",
      "status":"ACTIVE",
      "lastUpdatedDate":"2023-10-26T10:01:30.042687",
      "daysTotal":24,
      "effectiveDays":24,
      "taggedBail": {
        "caseSequence": 1
      }
   }
   ]
              """,
            ),
        ),
    )
  }

  fun stubCreate() {
    stubFor(
      post("/adjustments")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{}""".trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubEffectiveDaysForTaggedBailAdjustment() {
    stubFor(
      post("/adjustments/$REMAND_ID/effective-days")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{}""".trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
