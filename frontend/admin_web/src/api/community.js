/** 社区管理API */
import request from '@/utils/request'

// 帖子管理
export const getPostList = (params) => request.get('/admin/community/posts', { params })
export const getPostById = (id) => request.get(`/admin/community/posts/${id}`)
export const deletePost = (id) => request.delete(`/admin/community/posts/${id}`)
export const pinPost = (id) => request.post(`/admin/community/posts/${id}/pin`)
export const unpinPost = (id) => request.post(`/admin/community/posts/${id}/unpin`)
export const hidePost = (id) => request.post(`/admin/community/posts/${id}/hide`)
export const showPost = (id) => request.post(`/admin/community/posts/${id}/show`)

// 评论管理
export const getCommentList = (params) => request.get('/admin/community/comments', { params })
export const deleteComment = (id) => request.delete(`/admin/community/comments/${id}`)

// 统计
export const getCommunityStatistics = () => request.get('/admin/community/statistics')
