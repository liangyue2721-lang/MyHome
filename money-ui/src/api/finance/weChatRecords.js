import request from '@/utils/request'

// 查询微信支付宝流水列表
export function listWeChatRecords(query) {
  return request({
    url: '/finance/weChatRecords/list',
    method: 'get',
    params: query
  })
}

// 查询微信支付宝流水详细
export function getWeChatRecords(id) {
  return request({
    url: '/finance/weChatRecords/' + id,
    method: 'get'
  })
}

// 新增微信支付宝流水
export function addWeChatRecords(data) {
  return request({
    url: '/finance/weChatRecords',
    method: 'post',
    data: data
  })
}

// 修改微信支付宝流水
export function updateWeChatRecords(data) {
  return request({
    url: '/finance/weChatRecords',
    method: 'put',
    data: data
  })
}

// 删除微信支付宝流水
export function delWeChatRecords(id) {
  return request({
    url: '/finance/weChatRecords/' + id,
    method: 'delete'
  })
}

// 导入流水解析
export function importWeChatRecords(file, userId) {
  const formData = new FormData();
  formData.append('file', file); // 添加文件到 FormData
  formData.append('userId', userId);    // ✅ 添加用户ID

  return request({
    url: '/finance/weChatRecords/import', // 导入接口
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data' // 设置请求头
    }
  });
}
