import { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import SimulatorPage from './pages/SimulatorPage';
import ArchitecturePage from './pages/ArchitecturePage';
import AboutPage from './pages/AboutPage';
import TraceReplayPage from './pages/TraceReplayPage';
import { useSimulator } from './hooks/useSimulator';

export default function App() {
  const [page, setPage] = useState('simulator');

  // Top-level hook instance just for the backend status in the navbar
  const { backendOnline, checkBackend } = useSimulator();

  useEffect(() => {
    checkBackend();
  }, [checkBackend]);

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden', background: 'var(--bg-base)' }}>
      <Navbar
        activePage={page}
        onNavigate={setPage}
        backendOnline={backendOnline}
      />
      <main style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {page === 'simulator'    && <SimulatorPage />}
        {page === 'trace'         && <TraceReplayPage />}
        {page === 'architecture' && <ArchitecturePage />}
        {page === 'about'        && <AboutPage />}
      </main>
    </div>
  );
}
