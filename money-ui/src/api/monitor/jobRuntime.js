import request from '@/utils/request'

// 查询实时任务列表
export function listRuntime(query) {
  return request({
    url: '/quartz/runtime/list',
    method: 'get',
    params: query
  })
}

// 查询实时任务详细
export function getRuntime(id) {
  return request({
    url: '/quartz/runtime/' + id,
    method: 'get'
  })
}

// 删除实时任务
export function delRuntime(id) {
  return request({
    url: '/quartz/runtime/' + id,
    method: 'delete'
  })
}
