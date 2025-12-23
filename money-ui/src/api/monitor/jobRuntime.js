import request from '@/utils/request'

// 查询实时任务（待执行 / 执行中）列表
export function listRuntime(query) {
  return request({
    url: '/quartz/runtime/list',
    method: 'get',
    params: query
  })
}
