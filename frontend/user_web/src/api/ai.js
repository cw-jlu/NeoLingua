/** AI模型API */
import request from '@/utils/request'

export const getModels = () => request.get('/user/ai/models')
