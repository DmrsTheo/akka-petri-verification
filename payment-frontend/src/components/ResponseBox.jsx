// Affiche la réponse JSON de l'API avec un style selon le statut
export default function ResponseBox({ result }) {
  if (!result) {
    return <div className="resp-idle">— en attente d'une action</div>
  }
  return (
    <div className={result.ok ? 'resp-ok' : 'resp-err'}>
      {JSON.stringify(result.data, null, 2)}
    </div>
  )
}
