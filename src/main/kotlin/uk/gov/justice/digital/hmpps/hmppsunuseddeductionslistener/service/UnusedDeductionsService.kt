package uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.AdjustmentsApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.client.CalculateReleaseDatesApiClient
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.Adjustment
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentEffectiveDays
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentSource
import uk.gov.justice.digital.hmpps.hmppsunuseddeductionslistener.model.AdjustmentType
import kotlin.math.max

@Service
class UnusedDeductionsService(
  val adjustmentsApiClient: AdjustmentsApiClient,
  val calculateReleaseDatesApiClient: CalculateReleaseDatesApiClient,

) {

  fun recalculateUnusedDeductions(offenderNo: String) {
    val adjustments = adjustmentsApiClient.getAdjustmentsByPerson(offenderNo)
    val anyDpsAdjustments = adjustments.any { it.source == AdjustmentSource.DPS }
    if (anyDpsAdjustments) {
      log.info("Recalculating unused deductions from $offenderNo")
      val deductions = adjustments
        .filter { it.adjustmentType === AdjustmentType.REMAND || it.adjustmentType === AdjustmentType.TAGGED_BAIL }

      if (deductions.isEmpty()) {
        setUnusedDeductions(0, adjustments, deductions)
        return
      }

      val allDeductionsEnteredInDps = deductions.all { it.source == AdjustmentSource.DPS }

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
      val effectiveDays = max(it.days!! - remainingDeductions, 0)
      remainingDeductions -= it.days
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
            fromDate = null,
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
