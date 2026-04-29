<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PropertyService from '@/api/property'
import { uploadFile } from '@/api/upload'
import { getPropertyImage } from '@/utils/property'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const currentUser = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const isEditMode = computed(() => route.name === 'property-edit')
const modelUrlPattern = /\.(glb|gltf)(\?.*)?$/i
const modelUploading = ref(false)

const form = ref({
  title: '',
  description: '',
  price: null,
  address: '',
  imageUrl: '',
  floorPlanUrl: '',
  model3dUrl: '',
  panoramaUrl: '',
  type: '二手房',
  layout: '',
  area: null,
  floor: '',
  decoration: '精装修',
  orientation: '南向',
  yearBuilt: '',
  contactName: currentUser.value.username || '',
  tags: [],
  latitude: null,
  longitude: null,
  furnishingPlan: '',
})

const validateModelUrl = (_rule, value, callback) => {
  const normalized = String(value || '').trim()
  if (!normalized) {
    callback()
    return
  }

  if (normalized.startsWith('/models/') || modelUrlPattern.test(normalized)) {
    callback()
    return
  }

  callback(new Error('3D 模型地址需为 .glb/.gltf 或 /models/ 下的资源'))
}

const rules = {
  title: [{ required: true, message: '请输入房源标题', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  area: [{ required: true, message: '请输入建筑面积', trigger: 'blur' }],
  model3dUrl: [{ validator: validateModelUrl, trigger: 'blur' }],
}

const tagOptions = ['近地铁', '采光好', '学区房', '带车位', '精装修', '宠物友好', '拎包入住', '安静社区']

const hydrateEditData = async () => {
  if (!isEditMode.value) return

  try {
    const response = await PropertyService.getProperty(route.params.id)
    const data = response.data
    form.value = {
      ...form.value,
      ...data,
      tags: data.tags ? String(data.tags).split(',').filter(Boolean) : [],
      furnishingPlan: data.furnishingPlan || '',
    }
  } catch (error) {
    ElMessage.error('获取房源信息失败')
    router.push('/agent')
  }
}

const previewModel = () => {
  const normalized = String(form.value.model3dUrl || '').trim()
  if (!normalized) {
    ElMessage.info('请先填写 3D 模型地址')
    return
  }

  if (!(normalized.startsWith('/models/') || modelUrlPattern.test(normalized))) {
    ElMessage.warning('模型地址格式不正确，请使用 .glb/.gltf 或 /models/ 路径')
    return
  }

  window.open(normalized, '_blank', 'noopener,noreferrer')
}

const beforeModelUpload = (rawFile) => {
  const validType = modelUrlPattern.test(rawFile.name)
  if (!validType) {
    ElMessage.warning('只能上传 .glb 或 .gltf 模型文件')
    return false
  }

  const sizeInMb = rawFile.size / 1024 / 1024
  if (sizeInMb > 30) {
    ElMessage.warning('模型文件请控制在 30MB 以内')
    return false
  }

  return true
}

const handleModelUpload = async ({ file }) => {
  modelUploading.value = true
  try {
    const response = await uploadFile(file)
    form.value.model3dUrl = response.data?.url || ''
    ElMessage.success('3D 模型上传成功，地址已自动回填')
  } catch (error) {
    ElMessage.error(error?.response?.data || '3D 模型上传失败')
  } finally {
    modelUploading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const payload = {
      ...form.value,
      imageUrl: form.value.imageUrl || getPropertyImage({}, 1),
      tags: form.value.tags.join(','),
    }

    if (isEditMode.value) {
      await PropertyService.updateProperty(route.params.id, payload)
      ElMessage.success('房源信息已更新')
    } else {
      await PropertyService.createProperty(payload)
      ElMessage.success('房源发布成功，等待审核')
    }

    router.push(currentUser.value.role === 'AGENT' ? '/agent' : '/profile')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '提交失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  hydrateEditData()
})
</script>

<template>
  <div class="property-form-container">
    <div class="hero-card">
      <div>
        <h2>{{ isEditMode ? '编辑房源' : '发布新房源' }}</h2>
        <p>填写房源基础信息，上传户型图和 3D 模型，发布后即可展示给客户。</p>
      </div>
      <el-button round @click="router.back()">返回</el-button>
    </div>

    <el-card class="form-card" shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="20">
          <el-col :md="16" :xs="24">
            <el-form-item label="房源标题" prop="title">
              <el-input v-model="form.title" placeholder="例如：万科城精装三居，双阳台带大客厅" />
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="房源类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="二手房" value="二手房" />
                <el-option label="租房" value="租房" />
                <el-option label="新房" value="新房" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :md="12" :xs="24">
            <el-form-item label="详细地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入详细地址" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" placeholder="默认显示当前登录用户" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :md="6" :xs="12">
            <el-form-item label="价格（万）" prop="price">
              <el-input-number v-model="form.price" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :md="6" :xs="12">
            <el-form-item label="面积（㎡）" prop="area">
              <el-input-number v-model="form.area" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :md="6" :xs="12">
            <el-form-item label="户型">
              <el-input v-model="form.layout" placeholder="例如：3室2厅" />
            </el-form-item>
          </el-col>
          <el-col :md="6" :xs="12">
            <el-form-item label="楼层">
              <el-input v-model="form.floor" placeholder="例如：中层 / 18层" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :md="8" :xs="24">
            <el-form-item label="装修">
              <el-select v-model="form.decoration" style="width: 100%">
                <el-option label="毛坯" value="毛坯" />
                <el-option label="简装" value="简装" />
                <el-option label="精装修" value="精装修" />
                <el-option label="豪华装修" value="豪华装修" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="朝向">
              <el-select v-model="form.orientation" style="width: 100%">
                <el-option label="南向" value="南向" />
                <el-option label="南北通透" value="南北通透" />
                <el-option label="东向" value="东向" />
                <el-option label="西向" value="西向" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="建成年代">
              <el-input v-model="form.yearBuilt" placeholder="例如：2018" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :md="12" :xs="24">
            <el-form-item label="纬度">
              <el-input-number v-model="form.latitude" :step="0.0001" :precision="6" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="经度">
              <el-input-number v-model="form.longitude" :step="0.0001" :precision="6" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="房源标签">
          <el-checkbox-group v-model="form.tags">
            <el-checkbox v-for="tag in tagOptions" :key="tag" :label="tag" />
          </el-checkbox-group>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :md="8" :xs="24">
            <el-form-item label="封面图 URL">
              <el-input v-model="form.imageUrl" placeholder="留空会使用默认房源图" />
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="户型平面图 URL">
              <el-input v-model="form.floorPlanUrl" placeholder="软装编辑器优先读取这张底图" />
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="3D 模型 URL" prop="model3dUrl">
              <el-input v-model="form.model3dUrl" placeholder="例如：/models/house-interiors.glb" />
            </el-form-item>
            <div class="model-helper">
              <span>建议填写 `.glb` / `.gltf`，可直接用于 3D 看房模块。</span>
              <el-button link type="primary" @click="previewModel">检查模型地址</el-button>
            </div>
            <div class="model-upload-box">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeModelUpload"
                :http-request="handleModelUpload"
                accept=".glb,.gltf,model/gltf-binary,model/gltf+json"
              >
                <el-button :loading="modelUploading" plain>
                  {{ modelUploading ? '上传中...' : '上传本地 3D 模型' }}
                </el-button>
              </el-upload>
              <span>上传成功后会自动填写模型地址。</span>
            </div>
          </el-col>
        </el-row>

        <el-form-item label="全景资源 URL">
          <el-input v-model="form.panoramaUrl" placeholder="可选，用于详情页全景资源入口" />
        </el-form-item>

        <el-form-item label="房源描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="描述采光、通风、配套和居住感受，能明显提升咨询和预约率"
          />
        </el-form-item>

        <div class="preview-card">
          <div class="preview-label">预览卡片</div>
          <div class="preview-title">{{ form.title || '还没有填写标题' }}</div>
          <div class="preview-meta">
            <span>{{ form.layout || '3室2厅' }}</span>
            <span>{{ form.area || 0 }}㎡</span>
            <span>{{ form.orientation || '南向' }}</span>
          </div>
          <div class="preview-tags">
            <el-tag v-for="tag in form.tags.slice(0, 4)" :key="tag" round>{{ tag }}</el-tag>
          </div>
          <div class="preview-assets">
            <span :class="{ ready: !!form.floorPlanUrl }">平面图</span>
            <span :class="{ ready: !!form.model3dUrl }">3D 模型</span>
            <span :class="{ ready: !!form.panoramaUrl }">全景</span>
          </div>
        </div>

        <div class="form-actions">
          <el-button round @click="router.back()">取消</el-button>
          <el-button type="primary" round :loading="loading" @click="handleSubmit">
            {{ isEditMode ? '保存修改' : '提交房源' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.property-form-container {
  padding: 24px;
  max-width: 980px;
  margin: 0 auto;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  background: linear-gradient(135deg, #ffffff, #f0fbff);
  padding: 24px 28px;
  border-radius: 28px;
  box-shadow: 0 18px 40px rgba(79, 195, 247, 0.12);
  margin-bottom: 24px;
}

.hero-card h2 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #324b5c;
}

.hero-card p {
  margin: 0;
  color: #7a8b96;
}

.form-card {
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
}


.model-helper {
  margin-top: -8px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: #7b8b96;
  font-size: 12px;
  flex-wrap: wrap;
}

.model-upload-box {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  color: #7b8b96;
  font-size: 12px;
}

.preview-card {
  background: linear-gradient(135deg, #fff6fb, #eefcff);
  border-radius: 22px;
  padding: 20px;
  margin-top: 16px;
}

.preview-label {
  color: #f06292;
  font-weight: 700;
  margin-bottom: 8px;
}

.preview-title {
  font-size: 20px;
  font-weight: 800;
  color: #34495e;
  margin-bottom: 10px;
}

.preview-meta {
  display: flex;
  gap: 14px;
  color: #6b7d88;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.preview-tags,
.preview-assets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-assets {
  margin-top: 14px;
}

.preview-assets span {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(146, 165, 177, 0.16);
  color: #6c7d89;
  font-size: 12px;
}

.preview-assets span.ready {
  background: rgba(66, 185, 131, 0.16);
  color: #2e7d5b;
}

.form-actions {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
  gap: 14px;
}

@media (max-width: 640px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .model-helper {
    flex-direction: column;
    align-items: flex-start;
  }

  .model-upload-box {
    align-items: flex-start;
  }
}
</style>
