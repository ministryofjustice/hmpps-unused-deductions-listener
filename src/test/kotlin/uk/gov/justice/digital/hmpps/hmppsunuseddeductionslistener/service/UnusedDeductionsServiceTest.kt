package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.Adjustment
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentEffectiveDays
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentType
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.UnusedDeductionCalculationResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UnusedDeductionsServiceTest {

  @InjectMocks
  lateinit var unusedDeductionsService: UnusedDeductionsService
  private val adjustmentsApiClient = mock<AdjustmentsApiClient>()
  private val calculateReleaseDatesApiClient = mock<CalculateReleaseDatesApiClient>()

  @Test
  fun updateUnusedDeductions() {
    val person = "ABC123"
    val remand = Adjustment(
      UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
      LocalDate.now().minusDays(9), null, 90, LocalDateTime.now(), 90,
    )
    val taggedBail = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.TAGGED_BAIL, days = 90, daysBetween = null)
    val unusedDeductions = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS, days = 10, effectiveDays = 10, daysBetween = null)
    val adjustments = listOf(remand, taggedBail, unusedDeductions)

    whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
    whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(UnusedDeductionCalculationResponse(100))

    unusedDeductionsService.handleMessage(AdjustmentEvent(AdditionalInformation(id = UUID.randomUUID().toString(), offenderNo = person, source = "DPS", false)))

    verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 80, person))
    verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 0, person))
    verify(adjustmentsApiClient).updateAdjustment(unusedDeductions.copy(days = 100))
  }

  @Test
  fun updateUnusedDeductions_noExistingUnusedDeductionAdjustment() {
    val person = "ABC123"
    val remand = Adjustment(
      UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
      LocalDate.now().minusDays(9), null, 90, LocalDateTime.now(), 90,
    )
    val taggedBail = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.TAGGED_BAIL, days = 90, daysBetween = null)
    val adjustments = listOf(remand, taggedBail)

    whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
    whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(UnusedDeductionCalculationResponse(100))

    unusedDeductionsService.handleMessage(AdjustmentEvent(AdditionalInformation(id = UUID.randomUUID().toString(), offenderNo = person, source = "DPS", false)))

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
      LocalDate.now().minusDays(9), null, 90, LocalDateTime.now(), 80,
    )
    val taggedBail = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.TAGGED_BAIL, days = 90, daysBetween = null)
    val unusedDeductions = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS, days = 10, effectiveDays = 10, daysBetween = null)
    val adjustments = listOf(remand, taggedBail, unusedDeductions)

    whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
    whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(UnusedDeductionCalculationResponse(0))

    unusedDeductionsService.handleMessage(AdjustmentEvent(AdditionalInformation(id = UUID.randomUUID().toString(), offenderNo = person, source = "DPS", false)))

    verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 90, person))
    verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 90, person))
    verify(adjustmentsApiClient).deleteAdjustment(unusedDeductions.id!!)
  }

  @Test
  fun updateUnusedDeductions_NoDeductionsButUnusedDeductions() {
    val person = "ABC123"
    val unusedDeductions = Adjustment(
      UUID.randomUUID(), 1, 1, person, AdjustmentType.UNUSED_DEDUCTIONS, LocalDate.now().minusDays(100),
      LocalDate.now().minusDays(9), null, 90, LocalDateTime.now(), 90,
    )
    val adjustments = listOf(unusedDeductions)

    whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
    whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(UnusedDeductionCalculationResponse(0))

    unusedDeductionsService.handleMessage(AdjustmentEvent(AdditionalInformation(id = UUID.randomUUID().toString(), offenderNo = person, source = "DPS", false)))

    verify(adjustmentsApiClient).deleteAdjustment(unusedDeductions.id!!)
  }

  @Test
  fun unusedDeductions_wonthandleeventsthatarentlast() {
    unusedDeductionsService.handleMessage(AdjustmentEvent(AdditionalInformation(id = UUID.randomUUID().toString(), offenderNo = "ASD", source = "DPS", false, false)))

    verifyNoInteractions(adjustmentsApiClient)
  }
}
