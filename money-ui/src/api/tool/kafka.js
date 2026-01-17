import request from '@/utils/request'

// List topics
export function listTopics() {
  return request({
    url: '/tool/kafka/topics',
    method: 'get'
  })
}

// List consumer groups
export function listConsumers() {
  return request({
    url: '/tool/kafka/consumers',
    method: 'get'
  })
}

// Get details for specific consumer groups
export function getConsumerDetails(groupIds) {
  return request({
    url: '/tool/kafka/consumer/details',
    method: 'post',
    data: groupIds
  })
}

// Delete topic
export function deleteTopic(topicName) {
  return request({
    url: '/tool/kafka/topic/' + topicName,
    method: 'delete'
  })
}

// Delete all messages in a topic
export function deleteTopicMessages(topicName) {
  return request({
    url: '/tool/kafka/topic/' + topicName + '/messages',
    method: 'delete'
  })
}

// Get recent messages for a topic
export function getTopicMessages(topicName, count) {
  return request({
    url: '/tool/kafka/topic/' + topicName + '/messages',
    method: 'get',
    params: { count }
  })
}

// List stock tasks (paginated)
export function listStockTasks(query) {
  return request({
    url: '/monitor/stock-task/list',
    method: 'get',
    params: query
  })
}

// Reset Consumer Group Offset (Skip Backlog)
export function resetConsumerOffset(groupId, topic) {
  return request({
    url: '/tool/kafka/consumer/reset-offset',
    method: 'post',
    params: { groupId, topic }
  })
}

// Clear Redis Stock Status
export function clearStockStatus() {
  return request({
    url: '/tool/kafka/redis/clear-stock-status',
    method: 'post'
  })
}
