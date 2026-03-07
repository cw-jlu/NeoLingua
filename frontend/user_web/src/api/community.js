/** 社区API */
import request from '@/utils/request'

export const getPosts = (params) => request.get('/user/community/posts', { params })
export const getPost = (id) => request.get(`/user/community/posts/${id}`)
export const createPost = (data) => request.post('/user/community/posts', data)
export const deletePost = (id) => request.delete(`/user/community/posts/${id}`)
export const likePost = (id) => request.post(`/user/community/posts/${id}/like`)
export const favoritePost = (id) => request.post(`/user/community/posts/${id}/favorite`)
export const getComments = (postId, params) => request.get(`/user/community/posts/${postId}/comments`, { params })
export const createComment = (postId, data) => request.post(`/user/community/posts/${postId}/comments`, data)
export const deleteComment = (commentId) => request.delete(`/user/community/comments/${commentId}`)
