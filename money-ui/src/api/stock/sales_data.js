import request from '@/utils/request'

// 查询利润折线图数据列表
export function listSales_data(query) {
  return request({
    url: '/stock/sales_data/list',
    method: 'get',
    params: query
  })
}

// 查询利润折线图数据详细
export function getSales_data(id) {
  return request({
    url: '/stock/sales_data/' + id,
    method: 'get'
  })
}

// 新增利润折线图数据
export function addSales_data(data) {
  return request({
    url: '/stock/sales_data',
    method: 'post',
    data: data
  })
}

// 修改利润折线图数据
export function updateSales_data(data) {
  return request({
    url: '/stock/sales_data',
    method: 'put',
    data: data
  })
}

// 删除利润折线图数据
export function delSales_data(id) {
  return request({
    url: '/stock/sales_data/' + id,
    method: 'delete'
  })
}
