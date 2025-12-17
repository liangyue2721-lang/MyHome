import request from '@/utils/request'

// 查询婚礼支出记录列表
export function listWeddingExpense(query) {
    return request({
        url: '/finance/weddingExpense/list',
        method: 'get',
        params: query
    })
}

// 查询婚礼支出记录详细
export function getWeddingExpense(id) {
    return request({
        url: '/finance/weddingExpense/' + id,
        method: 'get'
    })
}

// 新增婚礼支出记录
export function addWeddingExpense(data) {
    return request({
        url: '/finance/weddingExpense',
        method: 'post',
        data: data
    })
}

// 修改婚礼支出记录
export function updateWeddingExpense(data) {
    return request({
        url: '/finance/weddingExpense',
        method: 'put',
        data: data
    })
}

// 删除婚礼支出记录
export function delWeddingExpense(id) {
    return request({
        url: '/finance/weddingExpense/' + id,
        method: 'delete'
    })
}
