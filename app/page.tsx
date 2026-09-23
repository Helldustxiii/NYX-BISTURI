export default function Home() {
  return (
    <main
      style={{
        padding: '2rem',
        fontFamily: 'system-ui, sans-serif',
        maxWidth: '800px',
        margin: '0 auto'
      }}
    >
      <h1>NYX-BISTURI</h1>
      <p>Espacio de trabajo para NYX / BISTURÍ</p>

      <section style={{ marginTop: '2rem' }}>
        <h2>Principio</h2>
        <blockquote
          style={{
            borderLeft: '4px solid #333',
            paddingLeft: '1rem',
            fontStyle: 'italic'
          }}
        >
          Primero entender y después actuar.
        </blockquote>
      </section>

      <section style={{ marginTop: '2rem' }}>
        <h2>Arquitectura</h2>
        <ul>
          <li><strong>BISTURÍ:</strong> comprensión, análisis, selección y decisión.</li>
          <li><strong>UMBRA:</strong> posibilidades, alternativas, hipótesis y conexiones.</li>
          <li><strong>NYX:</strong> contexto, identidad y expresión.</li>
        </ul>
        <p>
          No forman una cadena rígida. Se complementan según la situación.
        </p>
      </section>

      <section style={{ marginTop: '2rem' }}>
        <h2>Evidencia</h2>
        <ul>
          <li><strong>CONFIRMADO:</strong> respaldado por evidencia suficiente y verificable.</li>
          <li><strong>PLAUSIBLE:</strong> compatible con la evidencia, pero no confirmado.</li>
          <li><strong>FALLIDO-INCIERTO:</strong> contradicho, insuficiente o no verificable por ahora.</li>
        </ul>
        <p>
          Una explicación convincente no equivale a una explicación verdadera.
        </p>
      </section>

      <section style={{ marginTop: '2rem' }}>
        <h2>Enlaces</h2>
        <ul>
          <li>
            <a href="https://github.com/Helldustxiii/NYX-BISTURI">
              Repositorio GitHub
            </a>
          </li>
        </ul>
      </section>
    </main>
  )
}
