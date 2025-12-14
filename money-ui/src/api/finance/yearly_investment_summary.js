import request from '@/utils/request'

// 查询年度投资汇总列表
export function listYearly_investment_summary(query) {
  return request({
    url: '/finance/yearly_investment_summary/list',
    method: 'get',
    params: query
  })
}

// 查询年度投资汇总详细
export function getYearly_investment_summary(id) {
  return request({
    url: '/finance/yearly_investment_summary/' + id,
    method: 'get'
  })
}

// 新增年度投资汇总
export function addYearly_investment_summary(data) {
  return request({
    url: '/finance/yearly_investment_summary',
    method: 'post',
    data: data
  })
}

// 修改年度投资汇总
export function updateYearly_investment_summary(data) {
  return request({
    url: '/finance/yearly_investment_summary',
    method: 'put',
    data: data
  })
}

// 删除年度投资汇总
export function delYearly_investment_summary(id) {
  return request({
    url: '/finance/yearly_investment_summary/' + id,
    method: 'delete'
  })
}
