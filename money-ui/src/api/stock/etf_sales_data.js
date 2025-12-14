import request from '@/utils/request'

// 查询eft折线图数据列表
export function listEtf_sales_data(query) {
  return request({
    url: '/stock/etf_sales_data/list',
    method: 'get',
    params: query
  })
}

// 查询eft折线图数据详细
export function getEtf_sales_data(id) {
  return request({
    url: '/stock/etf_sales_data/' + id,
    method: 'get'
  })
}

// 新增eft折线图数据
export function addEtf_sales_data(data) {
  return request({
    url: '/stock/etf_sales_data',
    method: 'post',
    data: data
  })
}

// 修改eft折线图数据
export function updateEtf_sales_data(data) {
  return request({
    url: '/stock/etf_sales_data',
    method: 'put',
    data: data
  })
}

// 删除eft折线图数据
export function delEtf_sales_data(id) {
  return request({
    url: '/stock/etf_sales_data/' + id,
    method: 'delete'
  })
}
