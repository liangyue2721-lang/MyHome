import request from '@/utils/request'

// 查询东方财富股票列表
export function listStock_info_dongfang(query) {
  return request({
    url: '/stock/stock_info_dongfang/list',
    method: 'get',
    params: query
  })
}

// 查询东方财富股票详细
export function getStock_info_dongfang(id) {
  return request({
    url: '/stock/stock_info_dongfang/' + id,
    method: 'get'
  })
}

// 新增东方财富股票
export function addStock_info_dongfang(data) {
  return request({
    url: '/stock/stock_info_dongfang',
    method: 'post',
    data: data
  })
}

// 修改东方财富股票
export function updateStock_info_dongfang(data) {
  return request({
    url: '/stock/stock_info_dongfang',
    method: 'put',
    data: data
  })
}

// 删除东方财富股票
export function delStock_info_dongfang(id) {
  return request({
    url: '/stock/stock_info_dongfang/' + id,
    method: 'delete'
  })
}
