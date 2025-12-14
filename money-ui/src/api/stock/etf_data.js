import request from '@/utils/request'

// 查询ETF交易数据列表
export function listEtf_data(query) {
  return request({
    url: '/stock/etf_data/list',
    method: 'get',
    params: query
  })
}

// 查询ETF交易数据详细
export function getEtf_data(etfCode) {
  return request({
    url: '/stock/etf_data/' + etfCode,
    method: 'get'
  })
}

// 新增ETF交易数据
export function addEtf_data(data) {
  return request({
    url: '/stock/etf_data',
    method: 'post',
    data: data
  })
}

// 修改ETF交易数据
export function updateEtf_data(data) {
  return request({
    url: '/stock/etf_data',
    method: 'put',
    data: data
  })
}

// 删除ETF交易数据
export function delEtf_data(etfCode) {
  return request({
    url: '/stock/etf_data/' + etfCode,
    method: 'delete'
  })
}
