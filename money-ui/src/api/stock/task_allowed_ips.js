import request from '@/utils/request'

// 查询任务允许IP列表
export function listTask_allowed_ips(query) {
  return request({
    url: '/stock/task_allowed_ips/list',
    method: 'get',
    params: query
  })
}

// 查询任务允许IP详细
export function getTask_allowed_ips(id) {
  return request({
    url: '/stock/task_allowed_ips/' + id,
    method: 'get'
  })
}

// 新增任务允许IP
export function addTask_allowed_ips(data) {
  return request({
    url: '/stock/task_allowed_ips',
    method: 'post',
    data: data
  })
}

// 修改任务允许IP
export function updateTask_allowed_ips(data) {
  return request({
    url: '/stock/task_allowed_ips',
    method: 'put',
    data: data
  })
}

// 删除任务允许IP
export function delTask_allowed_ips(id) {
  return request({
    url: '/stock/task_allowed_ips/' + id,
    method: 'delete'
  })
}
