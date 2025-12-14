import request from '@/utils/request'

// 查询新股发行信息列表
export function listStock_issue_info(query) {
  return request({
    url: '/stock/stock_issue_info/list',
    method: 'get',
    params: query
  })
}

// 查询新股发行信息详细
export function getStock_issue_info(applyCode) {
  return request({
    url: '/stock/stock_issue_info/' + applyCode,
    method: 'get'
  })
}

// 新增新股发行信息
export function addStock_issue_info(data) {
  return request({
    url: '/stock/stock_issue_info',
    method: 'post',
    data: data
  })
}

// 修改新股发行信息
export function updateStock_issue_info(data) {
  return request({
    url: '/stock/stock_issue_info',
    method: 'put',
    data: data
  })
}

// 删除新股发行信息
export function delStock_issue_info(applyCode) {
  return request({
    url: '/stock/stock_issue_info/' + applyCode,
    method: 'delete'
  })
}
