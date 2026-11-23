import 'bootstrap/dist/css/bootstrap.min.css';
import '../../../shared/theme/tokens.css';
import React from 'react';
import ReactDOM from 'react-dom/client';
import '../../../shared/ui/site-shell.js';
import App from './App.jsx';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <fdl-site-shell active-path="/financial-data-lab/nhb-residex/">
      <App />
    </fdl-site-shell>
  </React.StrictMode>,
);
