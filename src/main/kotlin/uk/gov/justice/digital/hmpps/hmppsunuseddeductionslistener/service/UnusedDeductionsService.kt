package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.Adjustment
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentEffectiveDays
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentType
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.EditableAdjustmentDto
import kotlin.math.max

@Service
class UnusedDeductionsService(
  val adjustmentsApiClient: AdjustmentsApiClient,
  val calculateReleaseDatesApiClient: CalculateReleaseDatesApiClient,

) {

  fun handleMessage(adjustmentEvent: AdjustmentEvent) {
    log.info("Received message for adjustment change")
    val (_, offenderNo, source, unusedDeductions, lastEvent) = adjustmentEvent.additionalInformation
    if (source == "DPS" && !unusedDeductions && lastEvent) {
      val adjustments = adjustmentsApiClient.getAdjustmentsByPerson(offenderNo)
      val deductions = adjustments
        .filter { it.adjustmentType === AdjustmentType.REMAND || it.adjustmentType === AdjustmentType.TAGGED_BAIL }

      if (deductions.isEmpty()) {
        setUnusedDeductions(0, adjustments, deductions)
        return
      }

      val allDeductionsEnteredInDps = deductions.all { it.remand != null || it.taggedBail != null }

      if (allDeductionsEnteredInDps) {
        val calculatedUnusedDeductions =
          calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, offenderNo).unusedDeductions

        if (calculatedUnusedDeductions == null) {
          // Couldn't calculate.
          return
        }

        setUnusedDeductions(calculatedUnusedDeductions, adjustments, deductions)
        setEffectiveDays(calculatedUnusedDeductions, deductions)
      }
    }
  }

  private fun setEffectiveDays(unusedDeductions: Int, deductions: List<Adjustment>) {
    var remainingDeductions = unusedDeductions
    // Remand becomes unused first..
    deductions.sortedWith(compareBy({ it.adjustmentType.name }, { it.createdDate!! })).forEach {
      val effectiveDays = max(it.daysTotal - remainingDeductions, 0)
      remainingDeductions -= it.daysTotal
      remainingDeductions = max(remainingDeductions, 0)
      if (effectiveDays != it.effectiveDays) {
        adjustmentsApiClient.updateEffectiveDays(AdjustmentEffectiveDays(it.id!!, effectiveDays, it.person))
      }
    }
  }

  private fun setUnusedDeductions(
    unusedDeductions: Int,
    adjustments: List<Adjustment>,
    deductions: List<Adjustment>,
  ) {
    val unusedDeductionsAdjustment =
      adjustments.find { it.adjustmentType == AdjustmentType.UNUSED_DEDUCTIONS }
    if (unusedDeductionsAdjustment != null) {
      if (unusedDeductions == 0) {
        adjustmentsApiClient.deleteAdjustment(unusedDeductionsAdjustment.id!!)
      } else {
        if (unusedDeductionsAdjustment.daysTotal != unusedDeductions) {
          adjustmentsApiClient.updateAdjustment(toEditableAdjustment(unusedDeductionsAdjustment).copy(days = unusedDeductions))
        }
      }
    } else {
      if (unusedDeductions > 0) {
        val aDeduction = deductions[0]
        adjustmentsApiClient.createAdjustment(
          toEditableAdjustment(aDeduction).copy(
            id = null,
            fromDate = null,
            toDate = null,
            days = unusedDeductions,
            adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
          ),
        )
      }
    }
  }

  private fun toEditableAdjustment(adjustment: Adjustment): EditableAdjustmentDto {
    return EditableAdjustmentDto(
      id = adjustment.id,
      person = adjustment.person,
      days = adjustment.daysTotal,
      fromDate = adjustment.fromDate,
      toDate = adjustment.toDate,
      adjustmentType = adjustment.adjustmentType,
      sentenceSequence = adjustment.sentenceSequence,
      bookingId = adjustment.bookingId,
      prisonId = adjustment.prisonId,
    )
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

data class AdditionalInformation(
  val id: String,
  val offenderNo: String,
  val source: String,
  val unusedDeductions: Boolean = false,
  val lastEvent: Boolean = true,
)

data class AdjustmentEvent(
  val additionalInformation: AdditionalInformation,
)
