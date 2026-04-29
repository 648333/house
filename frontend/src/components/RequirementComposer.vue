<script setup>
import { computed, reactive } from 'vue'

const props = defineProps({
  activeCount: {
    type: Number,
    default: 0,
  },
  maxCount: {
    type: Number,
    default: 5,
  },
})

const emit = defineEmits(['submit'])

const scenarioOptions = [
  {
    key: 'commute',
    title: '通勤优先',
    subtitle: '近地铁、两居、节奏轻快',
    titleValue: '通勤友好两居',
    propertyType: '普通住宅',
    layoutPreference: '2室',
    area: '徐汇',
    budgetLabel: '300-500 万',
    lifestyleTags: ['近地铁', '通勤友好'],
  },
  {
    key: 'school',
    title: '学区改善',
    subtitle: '三居起步，社区成熟',
    titleValue: '学区改善三居',
    propertyType: '普通住宅',
    layoutPreference: '3室',
    area: '静安',
    budgetLabel: '500-800 万',
    lifestyleTags: ['学区优先', '改善型'],
  },
  {
    key: 'healing',
    title: '治愈独居',
    subtitle: '小而舒适，氛围感强',
    titleValue: '治愈风独居小宅',
    propertyType: '公寓',
    layoutPreference: '1室',
    area: '长宁',
    budgetLabel: '300 万以内',
    lifestyleTags: ['治愈风', '近地铁'],
  },
  {
    key: 'family',
    title: '家庭舒适',
    subtitle: '大空间、稳定居住',
    titleValue: '家庭舒适四居',
    propertyType: '普通住宅',
    layoutPreference: '4室及以上',
    area: '浦东',
    budgetLabel: '800 万以上',
    lifestyleTags: ['改善型', '家庭友好'],
  },
]

const areaOptions = ['徐汇', '静安', '长宁', '浦东', '闵行', '杨浦']
const typeOptions = ['普通住宅', '公寓', '新房', '别墅']
const layoutOptions = ['1室', '2室', '3室', '4室及以上']
const budgetOptions = ['300 万以内', '300-500 万', '500-800 万', '800 万以上']
const sceneOptions = ['近地铁', '学区优先', '通勤友好', '治愈风', '宠物友好', '改善型']

const form = reactive({
  scenario: 'commute',
  title: '通勤友好两居',
  preferredArea: '徐汇',
  propertyType: '普通住宅',
  layoutPreference: '2室',
  budgetLabel: '300-500 万',
  lifestyleTags: ['近地铁', '通勤友好'],
})

const isLimitReached = computed(() => props.activeCount >= props.maxCount)

const applyScenario = (scenario) => {
  form.scenario = scenario.key
  form.title = scenario.titleValue
  form.preferredArea = scenario.area
  form.propertyType = scenario.propertyType
  form.layoutPreference = scenario.layoutPreference
  form.budgetLabel = scenario.budgetLabel
  form.lifestyleTags = [...scenario.lifestyleTags]
}

const toggleScene = (scene) => {
  if (form.lifestyleTags.includes(scene)) {
    form.lifestyleTags = form.lifestyleTags.filter((item) => item !== scene)
    return
  }

  form.lifestyleTags = [...form.lifestyleTags, scene]
}

const submit = () => {
  emit('submit', {
    title: form.title,
    preferredArea: form.preferredArea,
    propertyType: form.propertyType,
    layoutPreference: form.layoutPreference,
    budgetLabel: form.budgetLabel,
    lifestyleTags: form.lifestyleTags,
  })
}
</script>

<template>
  <div class="composer-card">
    <div class="composer-head">
      <div>
        <span class="kicker">找房需求</span>
        <h3>先选一个方向，再细化偏好</h3>
        <p>控制在少量选择内，发布后会自动进入需求匹配区。</p>
      </div>
      <span class="limit-chip">进行中 {{ activeCount }}/{{ maxCount }}</span>
    </div>

    <div class="scenario-grid">
      <button
        v-for="item in scenarioOptions"
        :key="item.key"
        type="button"
        :class="['scenario-card', { active: form.scenario === item.key }]"
        @click="applyScenario(item)"
      >
        <strong>{{ item.title }}</strong>
        <span>{{ item.subtitle }}</span>
      </button>
    </div>

    <div class="field-grid">
      <div class="field-block">
        <span class="field-label">意向区域</span>
        <div class="chip-wrap">
          <button
            v-for="item in areaOptions"
            :key="item"
            type="button"
            :class="['choice-chip', { active: form.preferredArea === item }]"
            @click="form.preferredArea = item"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <div class="field-block">
        <span class="field-label">房源类型</span>
        <div class="chip-wrap">
          <button
            v-for="item in typeOptions"
            :key="item"
            type="button"
            :class="['choice-chip', { active: form.propertyType === item }]"
            @click="form.propertyType = item"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <div class="field-block">
        <span class="field-label">户型偏好</span>
        <div class="chip-wrap">
          <button
            v-for="item in layoutOptions"
            :key="item"
            type="button"
            :class="['choice-chip', { active: form.layoutPreference === item }]"
            @click="form.layoutPreference = item"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <div class="field-block">
        <span class="field-label">预算区间</span>
        <div class="chip-wrap">
          <button
            v-for="item in budgetOptions"
            :key="item"
            type="button"
            :class="['choice-chip', { active: form.budgetLabel === item }]"
            @click="form.budgetLabel = item"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <div class="field-block">
        <span class="field-label">生活偏好</span>
        <div class="chip-wrap">
          <button
            v-for="item in sceneOptions"
            :key="item"
            type="button"
            :class="['choice-chip', 'multi', { active: form.lifestyleTags.includes(item) }]"
            @click="toggleScene(item)"
          >
            {{ item }}
          </button>
        </div>
      </div>
    </div>

    <div class="summary-box">
      <span>{{ form.title }}</span>
      <span>{{ form.preferredArea }}</span>
      <span>{{ form.layoutPreference }}</span>
      <span>{{ form.budgetLabel }}</span>
    </div>

    <el-button type="primary" class="submit-btn" :disabled="isLimitReached" @click="submit">
      {{ isLimitReached ? '已达到发布上限' : '发布找房需求' }}
    </el-button>
  </div>
</template>

<style scoped>
.composer-card {
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.composer-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
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

.composer-head h3 {
  margin: 12px 0 6px;
  color: #30475a;
}

.composer-head p {
  margin: 0;
  color: #8093a3;
}

.limit-chip {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: #eef6ff;
  color: #5f7d97;
  font-size: 12px;
  font-weight: 700;
}

.scenario-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.scenario-card,
.choice-chip {
  border: none;
  background: #f4f8fd;
  color: #647f94;
  cursor: pointer;
  transition: all 0.2s ease;
}

.scenario-card {
  padding: 16px;
  border-radius: 20px;
  text-align: left;
}

.scenario-card strong,
.scenario-card span {
  display: block;
}

.scenario-card span {
  margin-top: 6px;
  font-size: 13px;
  color: #7f93a3;
}

.scenario-card.active,
.choice-chip.active {
  background: linear-gradient(135deg, #e8f5ff, #fff1f6);
  color: #3f84bb;
  box-shadow: 0 10px 20px rgba(111, 145, 183, 0.12);
}

.field-grid {
  margin-top: 20px;
  display: grid;
  gap: 16px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-label {
  color: #6d8295;
  font-size: 13px;
  font-weight: 700;
}

.chip-wrap {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.choice-chip {
  padding: 10px 14px;
  border-radius: 999px;
}

.summary-box {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.summary-box span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f3f8fe;
  color: #627d93;
  font-size: 13px;
}

.submit-btn {
  margin-top: 18px;
}

@media (max-width: 900px) {
  .scenario-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .composer-head,
  .scenario-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
