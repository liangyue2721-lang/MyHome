import request from '@/utils/request'

// 查询银行流水列表
export function listBankTransactions(query) {
  return request({
    url: '/finance/bankTransactions/list',
    method: 'get',
    params: query
  })
}

// 查询银行流水详细
export function getBankTransactions(id) {
  return request({
    url: '/finance/bankTransactions/' + id,
    method: 'get'
  })
}

// 新增银行流水
export function addBankTransactions(data) {
  return request({
    url: '/finance/bankTransactions',
    method: 'post',
    data: data
  })
}

// 修改银行流水
export function updateBankTransactions(data) {
  return request({
    url: '/finance/bankTransactions',
    method: 'put',
    data: data
  })
}

// 删除银行流水
export function delBankTransactions(id) {
  return request({
    url: '/finance/bankTransactions/' + id,
    method: 'delete'
  })
}

// 导入流水解析
export function importBankTransactions(file) {
  const formData = new FormData();
  formData.append('file', file); // 添加文件到 FormData

  return request({
    url: '/finance/bankTransactions/import', // 导入接口
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data' // 设置请求头
    }
  });
}
