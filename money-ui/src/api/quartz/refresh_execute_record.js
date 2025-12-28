import request from '@/utils/request'

// 查询刷新任务执行记录列表
export function listRefresh_execute_record(query) {
  return request({
    url: '/quartz/refresh_execute_record/list',
    method: 'get',
    params: query
  })
}

// 获取刷新任务执行统计 (Donut Chart)
export function getRefreshExecuteStats(query) {
  return request({
    url: '/quartz/refresh_execute_record/stats',
    method: 'get',
    params: query
  })
}

// 获取节点维度执行统计
export function getRefreshExecuteStatsByNodeIp() {
  return request({
    url: '/quartz/refresh_execute_record/stats/node_ip',
    method: 'get'
  })
}

// 查询刷新任务执行记录详细
export function getRefresh_execute_record(id) {
  return request({
    url: '/quartz/refresh_execute_record/' + id,
    method: 'get'
  })
}

// 新增刷新任务执行记录
export function addRefresh_execute_record(data) {
  return request({
    url: '/quartz/refresh_execute_record',
    method: 'post',
    data: data
  })
}

// 修改刷新任务执行记录
export function updateRefresh_execute_record(data) {
  return request({
    url: '/quartz/refresh_execute_record',
    method: 'put',
    data: data
  })
}

// 删除刷新任务执行记录
export function delRefresh_execute_record(id) {
  return request({
    url: '/quartz/refresh_execute_record/' + id,
    method: 'delete'
  })
}
