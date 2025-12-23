import request from '@/utils/request'

// 查询实时任务（待执行 / 执行中）列表
export function listRuntime(query) {
  return request({
    url: '/quartz/runtime/list',
    method: 'get',
    params: query
  })
}

// 查询实时任务（待执行 / 执行中）详细
export function getRuntime(id) {
  return request({
    url: '/quartz/runtime/' + id,
    method: 'get'
  })
}

// 新增实时任务（待执行 / 执行中）
export function addRuntime(data) {
  return request({
    url: '/quartz/runtime',
    method: 'post',
    data: data
  })
}

// 修改实时任务（待执行 / 执行中）
export function updateRuntime(data) {
  return request({
    url: '/quartz/runtime',
    method: 'put',
    data: data
  })
}

// 删除实时任务（待执行 / 执行中）
export function delRuntime(id) {
  return request({
    url: '/quartz/runtime/' + id,
    method: 'delete'
  })
}
