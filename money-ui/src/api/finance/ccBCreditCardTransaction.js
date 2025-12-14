import request from '@/utils/request'

// 查询建行信用卡交易记录列表
export function listCcBCreditCardTransaction(query) {
  return request({
    url: '/finance/ccb_credit_card_transaction/list',
    method: 'get',
    params: query
  })
}

// 查询建行信用卡交易记录详细
export function getCcBCreditCardTransaction(id) {
  return request({
    url: '/finance/ccb_credit_card_transaction/' + id,
    method: 'get'
  })
}

// 新增建行信用卡交易记录
export function addCcBCreditCardTransaction(data) {
  return request({
    url: '/finance/ccb_credit_card_transaction',
    method: 'post',
    data: data
  })
}

// 修改建行信用卡交易记录
export function updateCcBCreditCardTransaction(data) {
  return request({
    url: '/finance/ccb_credit_card_transaction',
    method: 'put',
    data: data
  })
}

// 删除建行信用卡交易记录
export function delCcBCreditCardTransaction(id) {
  return request({
    url: '/finance/ccb_credit_card_transaction/' + id,
    method: 'delete'
  })
}

// 查询建行信用卡交易记录详细
export function queryCCBCreditCardTransaction() {
  return request({
    url: '/finance/asset_record/handleQuerySync',
    method: 'get'
  })
}

// 导入流水解析
export function importCCBCreditCardTransaction(file) {
  const formData = new FormData();
  formData.append('file', file); // 添加文件到 FormData

  return request({
    url: '/finance/ccb_credit_card_transaction/import', // 导入接口
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data' // 设置请求头
    }
  });
}
