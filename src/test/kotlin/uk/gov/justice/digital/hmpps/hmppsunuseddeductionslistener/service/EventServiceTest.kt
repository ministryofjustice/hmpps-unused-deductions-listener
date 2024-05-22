package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.Adjustment
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentEffectiveDays
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentSource
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentType
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.RemandDto
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.TaggedBailDto
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.UnusedDeductionCalculationResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class EventServiceTest {

  private val adjustmentsApiClient = mock<AdjustmentsApiClient>()
  private val calculateReleaseDatesApiClient = mock<CalculateReleaseDatesApiClient>()

  private val unusedDeductionsService = UnusedDeductionsService(
    adjustmentsApiClient,
    calculateReleaseDatesApiClient,
  )

  private val eventService = EventService(
    unusedDeductionsService,
  )

  @Nested
  inner class AdjustmentEventTests {
    @Test
    fun updateUnusedDeductions() {
      val person = "ABC123"
      val remand = Adjustment(
        UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
        LocalDate.now().minusDays(9), LocalDateTime.now(), 90, 90, taggedBail = null, remand = RemandDto(listOf(1L)),
        source = AdjustmentSource.DPS,
      )
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val unusedDeductions = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        days = 10,
        effectiveDays = 10,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(100),
      )

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 80, person))
      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 0, person))
      verify(adjustmentsApiClient).updateAdjustment(unusedDeductions.copy(days = 100))
    }

    @Test
    fun updateUnusedDeductions_noExistingUnusedDeductionAdjustment() {
      val person = "ABC123"
      val remand = Adjustment(
        UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
        LocalDate.now().minusDays(9), LocalDateTime.now(), 90, 90, taggedBail = null, remand = RemandDto(listOf(1L)),
        source = AdjustmentSource.DPS,
      )
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val adjustments = listOf(remand, taggedBail)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(100),
      )

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 80, person))
      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 0, person))
      verify(adjustmentsApiClient).createAdjustment(
        remand.copy(
          id = null,
          toDate = null,
          fromDate = null,
          days = 100,
          adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        ),
      )
    }

    @Test
    fun updateUnusedDeductions_ZeroCalculatedDays() {
      val person = "ABC123"
      val remand = Adjustment(
        UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
        LocalDate.now().minusDays(9), LocalDateTime.now(), 90, 80, taggedBail = null, remand = RemandDto(listOf(1L)),
        source = AdjustmentSource.DPS,
      )
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val unusedDeductions = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        days = 10,
        effectiveDays = 10,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(0),
      )

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 90, person))
      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 90, person))
      verify(adjustmentsApiClient).deleteAdjustment(unusedDeductions.id!!)
    }

    @Test
    fun updateUnusedDeductions_NoDeductionsButUnusedDeductions() {
      val person = "ABC123"
      val unusedDeductions = Adjustment(
        UUID.randomUUID(), 1, 1, person, AdjustmentType.UNUSED_DEDUCTIONS, LocalDate.now().minusDays(100),
        LocalDate.now().minusDays(9), LocalDateTime.now(), 90, 90, taggedBail = null, remand = null,
        source = AdjustmentSource.DPS,
      )
      val adjustments = listOf(unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(0),
      )

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).deleteAdjustment(unusedDeductions.id!!)
    }

    @Test
    fun unusedDeductions_wonthandleeventsthatarentlast() {
      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = "ASD",
            source = "DPS",
            false,
            false,
          ),
        ),
      )

      verifyNoInteractions(adjustmentsApiClient)
    }
  }

  @Nested
  inner class GeneralTests {
    @Test
    fun `Do not update unused deductions if adjustments arent DPS`() {
      val person = "ABC123"
      val remand = Adjustment(
        UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
        LocalDate.now().minusDays(9), LocalDateTime.now(), 90, 90, taggedBail = null, remand = RemandDto(listOf(1L)),
        source = AdjustmentSource.NOMIS,
      )
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val unusedDeductions = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        days = 10,
        effectiveDays = 10,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)

      eventService.handleAdjustmentMessage(
        AdjustmentEvent(
          AdjustmentAdditionalInformation(
            id = UUID.randomUUID().toString(),
            offenderNo = person,
            source = "DPS",
            false,
          ),
        ),
      )

      verify(adjustmentsApiClient).getAdjustmentsByPerson(person)
      verifyNoMoreInteractions(adjustmentsApiClient)
    }
  }

  @Nested
  inner class PrisonerSearchTests {
    @Test
    fun `Handle prisoner event`() {
      val person = "ABC123"
      val remand = Adjustment(
        UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
        LocalDate.now().minusDays(9), LocalDateTime.now(), 90, 90, taggedBail = null, remand = RemandDto(listOf(1L)),
        source = AdjustmentSource.DPS,
      )
      val taggedBail = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.TAGGED_BAIL,
        taggedBail = TaggedBailDto(1),
        remand = null,
      )
      val unusedDeductions = remand.copy(
        id = UUID.randomUUID(),
        adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
        days = 10,
        effectiveDays = 10,
      )
      val adjustments = listOf(remand, taggedBail, unusedDeductions)

      whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
      whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
        UnusedDeductionCalculationResponse(100),
      )

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SENTENCE"),
          ),
        ),
      )

      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 80, person))
      verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 0, person))
      verify(adjustmentsApiClient).updateAdjustment(unusedDeductions.copy(days = 100))
    }

    @Test
    fun `Dont handle non sentence events`() {
      val person = "ABC123"

      eventService.handlePrisonerSearchEvent(
        PrisonerSearchEvent(
          PrisonerSearchAdditionalInformation(
            nomsNumber = person,
            categoriesChanged = listOf("SOMETHINGELSE"),
          ),
        ),
      )

      verifyNoInteractions(adjustmentsApiClient)
    }
  }
}
