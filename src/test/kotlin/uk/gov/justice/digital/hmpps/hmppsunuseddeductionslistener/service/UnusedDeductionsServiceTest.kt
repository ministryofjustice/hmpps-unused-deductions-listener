package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.Adjustment
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentEffectiveDays
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentType
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.EditableAdjustmentDto
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.RemandDto
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.TaggedBailDto
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.UnusedDeductionCalculationResponse
import java.time.LocalDate
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
      LocalDate.now().minusDays(9), 90, 90, "LMI", remand = RemandDto(listOf(1L)),
    )
    val taggedBail = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.TAGGED_BAIL, taggedBail = TaggedBailDto(1), remand = null)
    val unusedDeductions = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS, daysTotal = 10, effectiveDays = 10)
    val adjustments = listOf(remand, taggedBail, unusedDeductions)

    whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
    whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(UnusedDeductionCalculationResponse(100))

    unusedDeductionsService.handleMessage(AdjustmentEvent(AdditionalInformation(id = UUID.randomUUID().toString(), offenderNo = person, source = "DPS", false)))

    verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(taggedBail.id!!, 80, person))
    verify(adjustmentsApiClient).updateEffectiveDays(AdjustmentEffectiveDays(remand.id!!, 0, person))
    verify(adjustmentsApiClient).updateAdjustment(
      argThat { arg: EditableAdjustmentDto ->
        arg.days == 100
      },
    )
  }

  @Test
  fun updateUnusedDeductions_noExistingUnusedDeductionAdjustment() {
    val person = "ABC123"
    val remand = Adjustment(
      UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
      LocalDate.now().minusDays(9), 90, 90, "LMI", remand = RemandDto(listOf(1L)),
    )
    val taggedBail = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.TAGGED_BAIL, taggedBail = TaggedBailDto(1), remand = null)
    val adjustments = listOf(remand, taggedBail)

    whenever(adjustmentsApiClient.getAdjustmentsByPerson(person)).thenReturn(adjustments)
    whenever(calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, person)).thenReturn(
      UnusedDeductionCalculationResponse(100),
    )

    unusedDeductionsService.handleMessage(
      AdjustmentEvent(
        AdditionalInformation(
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
      argThat { arg: EditableAdjustmentDto ->
        arg.days == 100
      },
    )
  }

  @Test
  fun updateUnusedDeductions_ZeroCalculatedDays() {
    val person = "ABC123"
    val remand = Adjustment(
      UUID.randomUUID(), 1, 1, person, AdjustmentType.REMAND, LocalDate.now().minusDays(100),
      LocalDate.now().minusDays(9), 90, 80, "LMI", remand = RemandDto(listOf(1L)),
    )
    val taggedBail = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.TAGGED_BAIL, daysTotal = 90, taggedBail = TaggedBailDto(1), remand = null)
    val unusedDeductions = remand.copy(id = UUID.randomUUID(), adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS, daysTotal = 10, effectiveDays = 10)
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
      LocalDate.now().minusDays(9), 90, 90, "LMI",
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
