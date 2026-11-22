import React from "react";
import Greeting from "./components/Greeting";
import SummaryGrid from "./components/SummaryGrid";

function App() {
  return (
    <div className="site-page">
      <h1 className="h4 mb-2">Stock Screeners</h1>
      <Greeting name="Chirag" />
      <SummaryGrid />
    </div>
  );
}

export default App;
