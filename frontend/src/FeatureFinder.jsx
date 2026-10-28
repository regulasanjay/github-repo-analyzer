import { useEffect, useState } from 'react';
import { Search, X, Layers, Code2, ExternalLink, ArrowUpRight, Sparkles, Bookmark, Trash2 } from 'lucide-react';
import './feature-finder.css';
import './search-history.css';
import './feature-results.css';
import './saved-locations.css';

const API = import.meta.env.VITE_API_URL || 'http://localhost:8000';
const ideas = ['Authentication', 'API endpoints', 'Database writes', 'Payments', 'Error handling', 'Background jobs'];

export default function FeatureFinder({ repo, onClose, onAsk }) {
  const [data, setData] = useState(null);
  const [history, setHistory] = useState([]);
  const [savedLocations, setSavedLocations] = useState([]);
  const [savingKey, setSavingKey] = useState('');
  const [query, setQuery] = useState('');
  const [viewMode, setViewMode] = useState('search');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function refreshHistory() {
    try {
      const response = await fetch(`${API}/repositories/${repo.id}/search-history`);
      if (response.ok) setHistory(await response.json());
    } catch { /* Keep the explorer usable if history cannot be loaded. */ }
  }

  async function refreshSaved() {
    try {
      const response = await fetch(`${API}/repositories/${repo.id}/saved-locations`);
      if (response.ok) setSavedLocations(await response.json());
    } catch { /* Keep the explorer usable if saved locations cannot be loaded. */ }
  }

  async function toggleSave(item) {
    const key = `${item.file}:${item.startLine}:${item.endLine}`;
    setSavingKey(key); setError('');
    const saved = savedLocations.find(location => `${location.file}:${location.startLine}:${location.endLine}` === key);
    try {
      const response = saved
        ? await fetch(`${API}/repositories/${repo.id}/saved-locations/${saved.id}`, { method: 'DELETE' })
        : await fetch(`${API}/repositories/${repo.id}/saved-locations`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ file: item.file, startLine: item.startLine, endLine: item.endLine, note: query }) });
      if (!response.ok) { const body = await response.json().catch(() => ({})); throw new Error(body.error || 'Could not update saved locations'); }
      await refreshSaved();
    } catch (e) { setError(e.message); }
    finally { setSavingKey(''); }
  }

  async function load(term = '', mode = 'search') {
    setLoading(true); setError('');
    try {
      const response = term
        ? await fetch(`${API}/repositories/${repo.id}/explore`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: term, mode }) })
        : await fetch(`${API}/repositories/${repo.id}/explore`);
      const body = await response.json();
      if (!response.ok) throw new Error(body.error || 'Could not explore this repository');
      setData(body); if (term) refreshHistory();
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }

  useEffect(() => { load(); refreshHistory(); refreshSaved(); }, [repo.id]);

  function timeAgo(value) {
    const minutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (minutes < 1440) return `${Math.floor(minutes / 60)}h ago`;
    return `${Math.floor(minutes / 1440)}d ago`;
  }

  return <div className="explorer-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) onClose(); }}>
    <section className="explorer" role="dialog" aria-modal="true" aria-label="Repository feature finder">
      <header className="explorer-header"><div className="explorer-title-mark"><Layers size={18}/></div><div><div className="explorer-eyebrow">REPOSITORY INTELLIGENCE</div><h2>Feature finder</h2><p>Find where real product behavior lives in this codebase.</p></div><button className="explorer-close" onClick={onClose} aria-label="Close feature finder"><X size={18}/></button></header>
      <div className="explorer-scroll">
        <form className="feature-search" onSubmit={e => { e.preventDefault(); setViewMode('search'); load(query,'search'); }}><Search size={17}/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Find a feature, flow, or behavior…"/><button disabled={loading || !query.trim()}>Find files <ArrowUpRight size={15}/></button><button className="trace-button" type="button" disabled={loading || !query.trim()} onClick={()=>{setViewMode('trace');load(query,'trace');}}>Trace request <Layers size={14}/></button></form>
        <div className="query-ideas"><span>TRY</span>{ideas.map(item => <button key={item} onClick={() => { setQuery(item); setViewMode('search'); load(item,'search'); }} disabled={loading}>{item}</button>)}</div>
        {error && <div className="explorer-error">{error}</div>}
        {loading && <div className="explorer-loading"><span className="thinking"><i/><i/><i/></span> Mapping repository files…</div>}
        {!loading && data && <>
          {query && <div className="results-heading"><div><span className="explorer-eyebrow">{viewMode==='trace'?'LIKELY REQUEST PATH':'FEATURE SEARCH'}</span><h3>{data.results.length ? (viewMode==='trace'?`How “${query}” may move through the app`:`Places related to “${query}”`) : `No direct matches for “${query}”`}</h3><p>{data.results.length ? `${data.results.length} useful code locations, ranked by relevance.` : 'Try a shorter term, an alternate name, or one of the suggested feature areas.'}</p></div><div className="results-actions"><button className="overview-back" onClick={()=>{setQuery('');setViewMode('search');load();}}>Recent searches</button><button className="ask-about" onClick={() => onAsk(query)}>Ask AI about this <Sparkles size={15}/></button></div></div>}
          {query && viewMode==='trace' && data.flowStages?.length>0 && <div className="flow-stages">{data.flowStages.map(stage=><section className="flow-stage" key={stage.name}><div className="flow-stage-head"><strong>{stage.name}</strong><p>{stage.explanation}</p></div><div className="flow-stage-files">{stage.files.map((file,i)=><a href={file.url} target="_blank" rel="noreferrer" key={`${file.file}-${i}`}><Code2 size={13}/><span>{file.file}</span><small>L{file.startLine}</small><ExternalLink size={12}/></a>)}</div></section>)}</div>}
          {data.results.length > 0 && <div className="feature-results">{data.results.map((item, i) => {const key=`${item.file}:${item.startLine}:${item.endLine}`;const isSaved=savedLocations.some(location=>`${location.file}:${location.startLine}:${location.endLine}`===key);return <article className="feature-result" key={`${item.file}-${item.startLine}-${i}`}><div className="result-top"><span className="result-rank">{String(i + 1).padStart(2, '0')}</span><div className="result-file"><Code2 size={15}/><strong>{item.file}</strong></div><span className="language-chip">{item.language}</span><a href={item.url} target="_blank" rel="noreferrer" title="Open source on GitHub"><span>L{item.startLine}–{item.endLine}</span><ExternalLink size={13}/></a><button className={`bookmark-hit ${isSaved?'is-saved':''}`} title={isSaved?'Remove bookmark':'Save code location'} aria-label={isSaved?'Remove bookmark':'Save code location'} disabled={savingKey===key} onClick={()=>toggleSave(item)}><Bookmark size={14} fill={isSaved?'currentColor':'none'}/></button></div><p className="match-reason">{item.reason}</p><pre>{item.snippet}</pre></article>})}</div>}
          {!query && <>
            {savedLocations.length>0&&<section className="saved-locations"><div className="recent-head"><div><span className="explorer-eyebrow">YOUR CODE SHORTLIST</span><h3>Saved locations</h3></div><span>{savedLocations.length} saved</span></div><div className="saved-list">{savedLocations.map(item=><div className="saved-location" key={item.id}><a href={item.url} target="_blank" rel="noreferrer"><Code2 size={14}/><span>{item.file}</span><small>L{item.startLine}–L{item.endLine}</small><ExternalLink size={12}/></a><button title="Remove saved location" aria-label="Remove saved location" onClick={()=>toggleSave(item)}><Trash2 size={13}/></button></div>)}</div></section>}
            {history.length>0&&<section className="recent-searches"><div className="recent-head"><div><span className="explorer-eyebrow">PICK UP WHERE YOU LEFT OFF</span><h3>Recent searches</h3></div><span>Saved to this repository</span></div><div className="recent-list">{history.map(item=><button className="recent-search" key={item.id} onClick={()=>{setQuery(item.query);setViewMode(item.mode);load(item.query,item.mode);}}><span className="recent-search-icon"><Search size={13}/></span><span className="recent-query"><strong>{item.query}</strong><small>{item.mode==='trace'?'Request trace':'Feature search'} · {item.resultCount} results</small></span><time>{timeAgo(item.createdAt)}</time><ArrowUpRight size={13}/></button>)}</div></section>}
            <div className="explorer-section-title"><div><span className="explorer-eyebrow">QUICK ORIENTATION</span><h3>Get to know the codebase</h3></div><span>{data.fileCount.toLocaleString()} files · {data.chunkCount.toLocaleString()} code sections</span></div>
            <div className="overview-grid"><section className="overview-card language-card"><h4>Languages</h4>{data.languages.map(l => <div className="language-row" key={l.name}><span>{l.name}</span><strong>{l.files}</strong><div className="language-track"><i style={{width:`${Math.max(5, l.files / Math.max(1, data.fileCount) * 100)}%`}}/></div></div>)}</section>
              <section className="overview-card area-card"><h4>Where things live</h4>{data.areas.map(a => <details className="area-row" key={a.name}><summary><span>{a.name}</span><b>{a.files}</b></summary><small>{a.description}</small><div>{a.paths.map(path => <code key={path}>{path}</code>)}</div></details>)}</section></div>
            {data.entryPoints.length > 0 && <section className="entrypoints"><h4>Likely starting points</h4><p>These files often show how a request or app begins.</p><div>{data.entryPoints.map(e => <a href={e.url} target="_blank" rel="noreferrer" key={e.file}><Code2 size={14}/><span>{e.file}</span><small>L{e.line}</small><ExternalLink size={12}/></a>)}</div></section>}
          </>}
        </>}
      </div>
    </section>
  </div>;
}
