// SSE 客户端：四态事件（delta / result / question / error）回调封装
export function connectSse(url, { onDelta, onResult, onQuestion, onError }) {
  const es = new EventSource(url)
  es.addEventListener('delta', (e) => onDelta?.(JSON.parse(e.data)))
  es.addEventListener('result', (e) => onResult?.(JSON.parse(e.data)))
  es.addEventListener('question', (e) => onQuestion?.(JSON.parse(e.data)))
  es.addEventListener('error', (e) => {
    onError?.(e)
    es.close()
  })
  return es
}
