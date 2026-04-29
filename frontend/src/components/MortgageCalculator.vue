<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  totalPrice: {
    type: Number,
    default: 0,
  },
})

const form = ref({
  downPaymentRatio: 30,
  years: 30,
  annualRate: 3.85,
})

const loanAmount = computed(() => {
  const total = Number(props.totalPrice || 0) * 10000
  return total * (1 - form.value.downPaymentRatio / 100)
})

const monthlyRate = computed(() => form.value.annualRate / 100 / 12)
const totalMonths = computed(() => form.value.years * 12)

const monthlyPayment = computed(() => {
  const principal = loanAmount.value
  const rate = monthlyRate.value
  const months = totalMonths.value

  if (!principal || !rate || !months) {
    return 0
  }

  const factor = Math.pow(1 + rate, months)
  return (principal * rate * factor) / (factor - 1)
})

const totalPayment = computed(() => monthlyPayment.value * totalMonths.value)
const totalInterest = computed(() => totalPayment.value - loanAmount.value)

const formatCurrency = (value) => `¥${Math.round(value).toLocaleString('zh-CN')}`
</script>

<template>
  <div class="calculator-card">
    <div class="calculator-head">
      <div>
        <span class="kicker">置业工具</span>
        <h3>房贷测算</h3>
      </div>
      <div class="price-chip">总价约 {{ totalPrice || 0 }} 万</div>
    </div>

    <div class="control-grid">
      <div class="control-item">
        <span>首付比例</span>
        <el-slider v-model="form.downPaymentRatio" :min="15" :max="80" :step="5" show-input />
      </div>

      <div class="inline-grid">
        <div class="control-item">
          <span>贷款年限</span>
          <el-select v-model="form.years">
            <el-option :value="10" label="10 年" />
            <el-option :value="20" label="20 年" />
            <el-option :value="30" label="30 年" />
          </el-select>
        </div>

        <div class="control-item">
          <span>年化利率</span>
          <el-input-number v-model="form.annualRate" :min="2.5" :max="8" :step="0.05" :precision="2" />
        </div>
      </div>
    </div>

    <div class="result-grid">
      <div class="result-item">
        <span class="label">贷款本金</span>
        <strong>{{ formatCurrency(loanAmount) }}</strong>
      </div>
      <div class="result-item highlight">
        <span class="label">月供参考</span>
        <strong>{{ formatCurrency(monthlyPayment) }}</strong>
      </div>
      <div class="result-item">
        <span class="label">利息总额</span>
        <strong>{{ formatCurrency(totalInterest) }}</strong>
      </div>
      <div class="result-item">
        <span class="label">还款总额</span>
        <strong>{{ formatCurrency(totalPayment) }}</strong>
      </div>
    </div>
  </div>
</template>

<style scoped>
.calculator-card {
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(244, 250, 255, 0.92));
}

.calculator-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.kicker {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(79, 195, 247, 0.12);
  color: #4a9ad3;
  font-size: 12px;
  font-weight: 700;
}

.calculator-head h3 {
  margin: 12px 0 0;
  color: #294152;
}

.price-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: #edf6ff;
  color: #5f7b91;
  font-size: 13px;
}

.control-grid {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.control-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.control-item span {
  color: #728697;
  font-size: 13px;
}

.inline-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.result-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.result-item {
  padding: 16px;
  border-radius: 20px;
  background: white;
  border: 1px solid rgba(143, 177, 206, 0.16);
}

.result-item.highlight {
  background: linear-gradient(135deg, #e8f6ff, #fff2f6);
}

.label {
  display: block;
  color: #8a9eaf;
  font-size: 12px;
}

.result-item strong {
  display: block;
  margin-top: 8px;
  color: #2f465a;
  font-size: 20px;
}

@media (max-width: 640px) {
  .inline-grid,
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
