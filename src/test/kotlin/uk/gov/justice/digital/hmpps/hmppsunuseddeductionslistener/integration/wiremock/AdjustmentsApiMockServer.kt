package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.listener.ADJUSTMENT_ID
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.listener.OFFENDER_NUMBER

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
      "id":"29044a3c-6261-4681-af9e-ef2a84c51a22",
      "bookingId":1204935,
      "sentenceSequence":1,
      "person":"A1032DZ",
      "adjustmentType":"REMAND",
      "toDate":"2023-01-20",
      "fromDate":"2023-01-10",
      "days":null,
      "additionalDaysAwarded":null,
      "unlawfullyAtLarge":null,
      "prisonId":null,
      "prisonName":null,
      "lastUpdatedBy":"NOMIS",
      "status":"ACTIVE",
      "lastUpdatedDate":"2023-10-26T09:57:36.319803",
      "effectiveDays":11,
      "daysBetween":11
   },
   {
      "id":"$ADJUSTMENT_ID",
      "bookingId":1204935,
      "sentenceSequence":1,
      "person":"A1032DZ",
      "adjustmentType":"TAGGED_BAIL",
      "toDate":null,
      "fromDate":"2023-01-10",
      "days":24,
      "additionalDaysAwarded":null,
      "unlawfullyAtLarge":null,
      "prisonId":null,
      "prisonName":null,
      "lastUpdatedBy":"NOMIS",
      "status":"ACTIVE",
      "lastUpdatedDate":"2023-10-26T10:01:30.042687",
      "effectiveDays":24,
      "daysBetween":null
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
      post("/adjustments/$ADJUSTMENT_ID/effective-days")
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
