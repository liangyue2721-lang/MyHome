import request from '@/utils/request'

// 查询股票K线数据列表
export function listKline(query) {
  return request({
    url: '/stock/kline/list',
    method: 'get',
    params: query
  })
}

// 查询股票K线数据详细
export function getKline(id) {
  return request({
    url: '/stock/kline/' + id,
    method: 'get'
  })
}

// 新增股票K线数据
export function addKline(data) {
  return request({
    url: '/stock/kline',
    method: 'post',
    data: data
  })
}

// 修改股票K线数据
export function updateKline(data) {
  return request({
    url: '/stock/kline',
    method: 'put',
    data: data
  })
}

// 删除股票K线数据
export function delKline(id) {
  return request({
    url: '/stock/kline/' + id,
    method: 'delete'
  })
}
