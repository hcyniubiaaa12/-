// 埋点上报：三触点 RESULT_VIEW / SIM_REGISTER / REGISTER_SUCCESS，旁路不阻塞主流程
import http from '../api/http'

export function track(stage, { recordId, deptId }) {
  http.post('/track', { stage, recordId, deptId }).catch(() => {})
}
