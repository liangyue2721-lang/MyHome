import request from '@/utils/request'


// 获取饼型图数据
export function getRecordsPieChart() {
  return request({
    url: '/finance/pieChart/transactionTypePieChart',
    method: 'get'
  });
}

// 获取饼型图数据
export function getRecordsColumnChart() {
  return request({
    url: '/finance/pieChart/totalAmountPieChart',
    method: 'get'
  });
}

// 获取柱形图数据
export function getWechatAlipayData() {
  return request({
    url: '/finance/pieChart/getWechatAlipayData',
    method: 'get'
  });
}

// 获取柱型图数据
export function getTotalAmountChart() {
  return request({
    url: '/finance/pieChart/totalAmountChart',
    method: 'get'
  });
}

// 获取收入支出柱型图数据
export function getMonthlyExpenditureBarChart() {
  return request({
    url: '/finance/pieChart/totalMonthlyExpenditureBarChart',
    method: 'get'
  });
}

// 获取贷款总额偿还比例数据
export function getRepaymentPieChart() {
  return request({
    url: '/finance/pieChart/repaymentPieChart',
    method: 'get'
  });
}

// 获取贷款总额加利息偿还比例数据
export function getTotalRepaymentPieChart() {
  return request({
    url: '/finance/pieChart/totalRepaymentPieChart',
    method: 'get'
  });
}

// 获取当月消费收入比数据
export function getMonthIncomeExpenseRatio() {
  return request({
    url: '/finance/pieChart/getMonthIncomeExpenseRatio',
    method: 'get'
  });
}

// 获取当年消费收入比数据
export function getYearIncomeExpenseRatio() {
  return request({
    url: '/finance/pieChart/getYearIncomeExpenseRatio',
    method: 'get'
  });
}

// 获取利润折线图数据
export function getProfitLineData() {
  return request({
    url: '/finance/pieChart/getProfitLineData',
    method: 'get'
  });
}


// 获取利润折线图数据
export function getETFProfitLineData() {
  return request({
    url: '/finance/pieChart/getETFProfitLineData',
    method: 'get'
  });
}

// 获取每月的收入支出柱型图数据
export function getMonthlyIncomeBarChart() {
  return request({
    url: '/finance/pieChart/getMonthlyIncomeBarChart',
    method: 'get'
  });
}

// 获取每月的还贷柱型图数据
export function renderLoanRepaymentComparisonChart() {
  return request({
    url: '/finance/pieChart/renderLoanRepaymentComparisonChart',
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
