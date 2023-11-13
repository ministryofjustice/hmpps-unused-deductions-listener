package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration.wiremock.AdjustmentsApiExtension
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration.wiremock.CalculateReleaseDatesApiExtension
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.integration.wiremock.HmppsAuthApiExtension

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(CalculateReleaseDatesApiExtension::class, AdjustmentsApiExtension::class, HmppsAuthApiExtension::class)
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  companion object {
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { LocalStackContainer.setLocalStackProperties(it, registry) }
    }
  }
}
