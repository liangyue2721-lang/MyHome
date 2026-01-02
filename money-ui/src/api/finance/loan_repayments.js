import request from '@/utils/request'

// 查询贷款剩余计算列表
export function listLoanRepayments(query) {
  return request({
    url: '/finance/loan_repayments/list',
    method: 'get',
    params: query
  })
}

// 查询贷款剩余计算详细
export function getLoanRepayments(id) {
  return request({
    url: '/finance/loan_repayments/' + id,
    method: 'get'
  })
}

// 查询贷款剩余详细
export function getLoanRepaymentsLineChart() {
  return request({
    url: '/finance/loan_repayments/getLoanRepaymentsLineChart',
    method: 'get'
  })
}

// 新增贷款剩余计算
export function addLoanRepayments(data) {
  return request({
    url: '/finance/loan_repayments',
    method: 'post',
    data: data
  })
}

// 修改贷款剩余计算
export function updateLoanRepayments(data) {
  return request({
    url: '/finance/loan_repayments',
    method: 'put',
    data: data
  })
}

// 删除贷款剩余计算
export function delLoanRepayments(id) {
  return request({
    url: '/finance/loan_repayments/' + id,
    method: 'delete'
  })
}

// 导入最新贷款金额
export function importRepayments(file) {
  const formData = new FormData();
  formData.append('file', file); // 添加文件到 FormData

  return request({
    url: '/finance/loan_repayments/import', // 导入接口
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data' // 设置请求头
    }
  });
}
