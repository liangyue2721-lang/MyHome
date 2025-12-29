import request from '@/utils/request'

// 查询年度存款统计列表
export function listAnnual_deposit_summary(query) {
  return request({
    url: '/finance/annual_deposit_summary/list',
    method: 'get',
    params: query
  })
}

// 查询用户当前年度存款统计
export function getAnnualSummary() {
  return request({
    url: '/finance/annual_deposit_summary/current-user-summary',
    method: 'get'
  })
}

// 查询年度存款统计详细
export function getAnnual_deposit_summary(id) {
  return request({
    url: '/finance/annual_deposit_summary/' + id,
    method: 'get'
  })
}

// 新增年度存款统计
export function addAnnual_deposit_summary(data) {
  return request({
    url: '/finance/annual_deposit_summary',
    method: 'post',
    data: data
  })
}

// 修改年度存款统计
export function updateAnnual_deposit_summary(data) {
  return request({
    url: '/finance/annual_deposit_summary',
    method: 'put',
    data: data
  })
}

// 删除年度存款统计
export function delAnnual_deposit_summary(id) {
  return request({
    url: '/finance/annual_deposit_summary/' + id,
    method: 'delete'
  })
}
