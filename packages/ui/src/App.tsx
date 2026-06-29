import './App.css';

const APP_TITLE = 'Queriva';
const STATUS_MESSAGE = 'Implementation in progress — see docs/SPEC.md';

/**
 * Root application component for the Queriva standalone SPA.
 */
function App() {
  return (
    <main className="app">
      <h1>{APP_TITLE}</h1>
      <p>{STATUS_MESSAGE}</p>
    </main>
  );
}

export default App;
