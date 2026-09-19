// 埋点上报（链路 C）：前端只报 result_view / sim_register 两个触点，
// register_success 由后端挂号确认接口补记——旁路信号，失败静默不阻塞主流程
import http from '../api/http'

export function track(stage, { recordId, deptId }) {
  http.post('/track', { stage, recordId, deptId }).catch(() => {})
}
