import request from '@/utils/request'


// 获取饼型图数据
export function getRecordsPieChart(id) {
  return request({
    url: '/finance/pieChart/transactionTypePieChart/' + id,
    method: 'get'
  });
}

// 获取饼型图数据
export function getRecordsColumnChart(id) {
  return request({
    url: '/finance/pieChart/totalAmountPieChart/' + id,
    method: 'get'
  });
}

// 获取柱形图数据
export function getWechatAlipayData(id) {
  return request({
    url: '/finance/pieChart/getWechatAlipayData/' + id,
    method: 'get'
  });
}

// 获取柱型图数据
export function getTotalAmountChart(id) {
  return request({
    url: '/finance/pieChart/totalAmountChart/' + id,
    method: 'get'
  });
}

// 获取收入支出柱型图数据
export function getMonthlyExpenditureBarChart(id) {
  return request({
    url: '/finance/pieChart/totalMonthlyExpenditureBarChart/' + id,
    method: 'get'
  });
}

// 获取贷款总额偿还比例数据
export function getRepaymentPieChart(id) {
  return request({
    url: '/finance/pieChart/repaymentPieChart/' + id,
    method: 'get'
  });
}

// 获取贷款总额加利息偿还比例数据
export function getTotalRepaymentPieChart(id) {
  return request({
    url: '/finance/pieChart/totalRepaymentPieChart/' + id,
    method: 'get'
  });
}

// 获取当月消费收入比数据
export function getMonthIncomeExpenseRatio(id) {
  return request({
    url: '/finance/pieChart/getMonthIncomeExpenseRatio/' + id,
    method: 'get'
  });
}

// 获取当年消费收入比数据
export function getYearIncomeExpenseRatio(id) {
  return request({
    url: '/finance/pieChart/getYearIncomeExpenseRatio/' + id,
    method: 'get'
  });
}

// 获取利润折线图数据
export function getProfitLineData(id) {
  return request({
    url: '/finance/pieChart/getProfitLineData/' + id,
    method: 'get'
  });
}


// 获取利润折线图数据
export function getETFProfitLineData(id) {
  return request({
    url: '/finance/pieChart/getETFProfitLineData/' + id,
    method: 'get'
  });
}

// 获取每月的收入支出柱型图数据
export function getMonthlyIncomeBarChart(id) {
  return request({
    url: '/finance/pieChart/getMonthlyIncomeBarChart/' + id,
    method: 'get'
  });
}

// 获取每月的还贷柱型图数据
export function renderLoanRepaymentComparisonChart(id) {
  return request({
    url: '/finance/pieChart/renderLoanRepaymentComparisonChart/' + id,
    method: 'get'
  });
}

// 获取有效服务器状态
export function GetLicenseCheck() {
  return request({
    url: '/finance/pieChart/GetLicenseCheck/',
    method: 'get'
  });
}
