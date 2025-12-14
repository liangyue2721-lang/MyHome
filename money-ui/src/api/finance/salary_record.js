import request from '@/utils/request'

// 查询员工工资明细列表
export function listSalary_record(query) {
  return request({
    url: '/finance/salary_record/list',
    method: 'get',
    params: query
  })
}

// 查询员工工资明细详细
export function getSalary_record(id) {
  return request({
    url: '/finance/salary_record/' + id,
    method: 'get'
  })
}

// 新增员工工资明细
export function addSalary_record(data) {
  return request({
    url: '/finance/salary_record',
    method: 'post',
    data: data
  })
}

// 修改员工工资明细
export function updateSalary_record(data) {
  return request({
    url: '/finance/salary_record',
    method: 'put',
    data: data
  })
}

// 删除员工工资明细
export function delSalary_record(id) {
  return request({
    url: '/finance/salary_record/' + id,
    method: 'delete'
  })
}

// 复制单条员工工资明细
export function copySalary_record(id) {
  return request({
    url: '/finance/salary_record/' + id,
    method: 'post'
  })
}
