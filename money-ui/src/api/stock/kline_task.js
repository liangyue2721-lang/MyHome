import request from '@/utils/request'

// 查询股票K线数据任务列表
export function listKline_task(query) {
  return request({
    url: '/stock/kline_task/list',
    method: 'get',
    params: query
  })
}

// 查询股票K线数据任务详细
export function getKline_task(id) {
  return request({
    url: '/stock/kline_task/' + id,
    method: 'get'
  })
}

// 新增股票K线数据任务
export function addKline_task(data) {
  return request({
    url: '/stock/kline_task',
    method: 'post',
    data: data
  })
}

// 修改股票K线数据任务
export function updateKline_task(data) {
  return request({
    url: '/stock/kline_task',
    method: 'put',
    data: data
  })
}

// 删除股票K线数据任务
export function delKline_task(id) {
  return request({
    url: '/stock/kline_task/' + id,
    method: 'delete'
  })
}
