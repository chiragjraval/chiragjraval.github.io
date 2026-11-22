import React from "react";

function Greeting({ name }) {
  return (
    <p className="small mb-3">
      Built by <strong>{name}</strong>.{" "}
      <a
        href="https://www.screener.in/screens/2618573/companies-with-growth-salesprofitmargin"
        target="_blank"
        rel="noreferrer"
      >
        View source screen on Screener.in
      </a>
    </p>
  );
}

export default Greeting;
