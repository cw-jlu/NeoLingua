/** Meeting API */
import request from '@/utils/request'

// 好友
export const getFriends = () => request.get('/user/friends')
export const searchUser = (keyword) => request.get('/user/friends/search', { params: { keyword } })
export const sendFriendRequest = (targetId) => request.post(`/user/friends/request/${targetId}`)
export const acceptFriend = (requestId) => request.post(`/user/friends/accept/${requestId}`)
export const rejectFriend = (requestId) => request.post(`/user/friends/reject/${requestId}`)
export const deleteFriend = (friendId) => request.delete(`/user/friends/${friendId}`)
export const getPendingRequests = () => request.get('/user/friends/pending')

// Meeting
export const createMeeting = (data) => request.post('/user/meetings', data)
export const getMyMeetings = () => request.get('/user/meetings')
export const getMeeting = (id) => request.get(`/user/meetings/${id}`)
export const joinMeeting = (id) => request.post(`/user/meetings/${id}/join`)
export const leaveMeeting = (id) => request.post(`/user/meetings/${id}/leave`)
export const endMeeting = (id) => request.post(`/user/meetings/${id}/end`)
export const inviteFriend = (meetingId, friendId) => request.post(`/user/meetings/${meetingId}/invite/${friendId}`)
export const addAiParticipant = (meetingId, roleId, aiName) => request.post(`/user/meetings/${meetingId}/add-ai`, null, { params: { roleId, aiName } })
export const removeParticipant = (meetingId, participantId) => request.delete(`/user/meetings/${meetingId}/participants/${participantId}`)
export const updateAiParticipant = (meetingId, participantId, data) => request.put(`/user/meetings/${meetingId}/participants/${participantId}`, data)
export const getAiRoles = () => request.get('/user/meetings/ai-roles')
