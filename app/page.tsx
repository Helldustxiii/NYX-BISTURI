export default function Home() {
  return (
    <main style={{ 
      padding: '2rem', 
      fontFamily: 'system-ui, sans-serif',
      maxWidth: '800px',
      margin: '0 auto'
    }}>
      <h1>NYX-BISTURI</h1>
      <p>Espacio de trabajo para NYX / BISTURÍ</p>
      
      <section style={{ marginTop: '2rem' }}>
        <h2>Principio</h2>
        <blockquote style={{ 
          borderLeft: '4px solid #333',
          paddingLeft: '1rem',
          fontStyle: 'italic'
        }}>
          Primero comprender. Después decidir.
        </blockquote>
      </section>

      <section style={{ marginTop: '2rem' }}>
        <h2>Arquitectura</h2>
        <ul>
          <li><strong>BISTURÍ:</strong> análisis, selección, criterio, filtro y revisión final.</li>
          <li><strong>UMBRA:</strong> exploración creativa y generación de posibilidades.</li>
          <li><strong>NYX:</strong> identidad, expresión y lectura del contexto conversacional.</li>
        </ul>
      </section>

      <section style={{ marginTop: '2rem' }}>
        <h2>Enlaces</h2>
        <ul>
          <li><a href="https://github.com/Helldustxiii/NYX-BISTURI">Repositorio GitHub</a></li>
        </ul>
      </section>
    </main>
  )
}
