export default function RouteTag({ method, path }) {
  const color = method === 'GET' ? 'badge-get' : 'badge-post'
  return (
    <div className="flex items-center gap-2 mb-4">
      <span className={`tag-method ${color}`}>{method}</span>
      <code className="text-xs text-stone-400 font-mono">{path}</code>
    </div>
  )
}
