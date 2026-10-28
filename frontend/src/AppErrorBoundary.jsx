import React from 'react';

export default class AppErrorBoundary extends React.Component {
  state = { error: null, componentStack: '' };

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('RepoMind interface render failed:', error);
    this.setState({ componentStack: info.componentStack || '' });
  }

  render() {
    if (this.state.error) {
      return <main className="fatal-error"><h1>RepoMind ran into a display error</h1><p>Your repository and chat are still safe. Reload the page to try again.</p><details><summary>Technical details</summary><pre>{String(this.state.error)}{this.state.componentStack}</pre></details><button onClick={() => window.location.reload()}>Reload RepoMind</button></main>;
    }
    return this.props.children;
  }
}
