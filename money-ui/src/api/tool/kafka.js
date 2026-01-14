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
