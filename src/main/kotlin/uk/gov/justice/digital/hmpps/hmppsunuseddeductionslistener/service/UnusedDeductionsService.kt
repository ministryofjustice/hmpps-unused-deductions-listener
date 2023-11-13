package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.Adjustment
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentEffectiveDays
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentType
import kotlin.math.max

@Service
class UnusedDeductionsService(
  val adjustmentsApiClient: AdjustmentsApiClient,
  val calculateReleaseDatesApiClient: CalculateReleaseDatesApiClient,

) {

  fun handleMessage(adjustmentEvent: AdjustmentEvent) {
    log.info("Received message for adjustment change")
    val (_, offenderNo, source, effectiveDays) = adjustmentEvent.additionalInformation
    if (source == "DPS" && !effectiveDays) {
      val adjustments = adjustmentsApiClient.getAdjustmentsByPerson(offenderNo)
      val deductions = adjustments
        .filter { it.adjustmentType === AdjustmentType.REMAND || it.adjustmentType === AdjustmentType.TAGGED_BAIL }

      if (deductions.isEmpty()) {
        setUnusedDeductions(0, adjustments, deductions)
        return
      }

      val allDeductionsEnteredInDps = deductions.all { it.days != null || it.daysBetween != null }

      if (allDeductionsEnteredInDps) {
        val (unusedDeductions) =
          calculateReleaseDatesApiClient.calculateUnusedDeductions(adjustments, offenderNo)

        if (unusedDeductions == null) {
          // Couldn't calculate.
          return
        }

        setUnusedDeductions(unusedDeductions, adjustments, deductions)
        setEffectiveDays(unusedDeductions, deductions)
      }
    }
  }

  private fun setEffectiveDays(unusedDeductions: Int, deductions: List<Adjustment>) {
    var remainingDeductions = unusedDeductions
    // Tagged bail first.
    deductions.sortedByDescending { it.adjustmentType.name }.forEach {
      val days = if (it.adjustmentType == AdjustmentType.TAGGED_BAIL) {
        it.days!!
      } else {
        it.daysBetween!!
      }
      val effectiveDays = max(days - remainingDeductions, 0)
      remainingDeductions -= days
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
        if (unusedDeductionsAdjustment.days != unusedDeductions) {
          adjustmentsApiClient.updateAdjustment(unusedDeductionsAdjustment.copy(days = unusedDeductions))
        }
      }
    } else {
      if (unusedDeductions > 0) {
        val aDeduction = deductions[0]
        adjustmentsApiClient.createAdjustment(
          aDeduction.copy(
            id = null,
            toDate = null,
            days = unusedDeductions,
            adjustmentType = AdjustmentType.UNUSED_DEDUCTIONS,
          ),
        )
      }
    }
  }

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

data class AdditionalInformation(
  val id: String,
  val offenderNo: String,
  val source: String,
  val effectiveDays: Boolean = false,
)

data class AdjustmentEvent(
  val additionalInformation: AdditionalInformation,
)
