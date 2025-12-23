import request from '@/utils/request'

// 查询任务执行历史记录列表
export function listLog(query) {
  return request({
    url: '/quartz/log/list',
    method: 'get',
    params: query
  })
}

// 查询任务执行历史记录详细
export function getLog(id) {
  return request({
    url: '/quartz/log/' + id,
    method: 'get'
  })
}

// 新增任务执行历史记录
export function addLog(data) {
  return request({
    url: '/quartz/log',
    method: 'post',
    data: data
  })
}

// 修改任务执行历史记录
export function updateLog(data) {
  return request({
    url: '/quartz/log',
    method: 'put',
    data: data
  })
}

// 删除任务执行历史记录
export function delLog(id) {
  return request({
    url: '/quartz/log/' + id,
    method: 'delete'
  })
}
