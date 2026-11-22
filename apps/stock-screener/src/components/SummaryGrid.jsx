import React, { useEffect, useState, useMemo } from "react";
import CompanyDetails from "./CompanyDetails";
import { stockScreenerDataBaseUrl } from "../config/data";

function SummaryGrid() {
  const [generatedAt, setGeneratedAt] = useState(new Date());
  const [data, setData] = useState([]);
  const [loadStatus, setLoadStatus] = useState("loading");
  const [loadError, setLoadError] = useState("");
  const [sortConfig, setSortConfig] = useState({ key: "totalScore", direction: "desc" });
  const [expandedRows, setExpandedRows] = useState(new Set());
  const [filters, setFilters] = useState({
    marketCapMin: "",
    marketCapMax: "",
    peRatioMin: "",
    peRatioMax: "",
    fundamentalScoreMin: "",
    fundamentalScoreMax: "",
    technicalScoreMin: "",
    technicalScoreMax: "",
    totalScoreMin: "",
    totalScoreMax: "",
    companyName: ""
  });

  useEffect(() => {
    const controller = new AbortController();

    async function loadSummary() {
      try {
        const response = await fetch(`${stockScreenerDataBaseUrl}summary.json`, {
          signal: controller.signal,
        });
        if (!response.ok) {
          throw new Error(`Request failed with HTTP ${response.status}`);
        }

        const json = await response.json();
        if (!Array.isArray(json.companies) || typeof json.generatedAt !== "string") {
          throw new Error("The stock summary has an invalid format");
        }

        setData(json.companies);
        setGeneratedAt(parseIsoTimestampWithNanos(json.generatedAt));
        setLoadStatus("loaded");
      } catch (error) {
        if (error.name === "AbortError") {
          return;
        }

        console.error("Unable to load stock summary:", error);
        setLoadError(error.message);
        setLoadStatus("error");
      }
    }

    loadSummary();
    return () => controller.abort();
  }, []);

  const getValue = (item, key) => {
    switch (key) {
      case "marketCap": return item.attributes.marketCap;
      case "peRatio": return item.attributes.peRatio;
      case "technicalScore": return item.companyScore.totalTechnicalScore;
      case "fundamentalScore": return item.companyScore.totalFundamentalScore;
      case "totalScore": return item.companyScore.totalScore;
      default: return item[key];
    }
  };

  const parseIsoTimestampWithNanos = (isoString) => {
    const [datePart, timePart] = isoString.split('T');
    const [time, nanoPart] = timePart.split('.');
    const [year, month, day] = datePart.split('-').map(Number);
    const [hours, minutes, seconds] = time.split(':').map(Number);
    const milliseconds = nanoPart ? Math.floor(Number('0.' + nanoPart) * 1000) : 0;
    return new Date(Date.UTC(year, month - 1, day, hours, minutes, seconds, milliseconds));
  }

  const filteredData = useMemo(() => {
    return data.filter((item) => {
      const marketCap = item.attributes.marketCap;
      const peRatio = item.attributes.peRatio;
      const techScore = item.companyScore.totalTechnicalScore;
      const fundScore = item.companyScore.totalFundamentalScore;
      const totalScore = item.companyScore.totalScore;

      if (filters.marketCapMin && marketCap < parseFloat(filters.marketCapMin)) return false;
      if (filters.marketCapMax && marketCap > parseFloat(filters.marketCapMax)) return false;
      if (filters.peRatioMin && peRatio < parseFloat(filters.peRatioMin)) return false;
      if (filters.peRatioMax && peRatio > parseFloat(filters.peRatioMax)) return false;
      if (filters.technicalScoreMin && techScore < parseFloat(filters.technicalScoreMin)) return false;
      if (filters.technicalScoreMax && techScore > parseFloat(filters.technicalScoreMax)) return false;
      if (filters.fundamentalScoreMin && fundScore < parseFloat(filters.fundamentalScoreMin)) return false;
      if (filters.fundamentalScoreMax && fundScore > parseFloat(filters.fundamentalScoreMax)) return false;
      if (filters.totalScoreMin && totalScore < parseFloat(filters.totalScoreMin)) return false;
      if (filters.totalScoreMax && totalScore > parseFloat(filters.totalScoreMax)) return false;
      if (filters.companyName && !item.companyName.toLowerCase().includes(filters.companyName.toLowerCase())) return false;

      return true;
    });
  }, [data, filters]);

  const sortedData = useMemo(() => {
    let sortableData = [...filteredData];
    if (sortConfig.key !== null) {
      sortableData.sort((a, b) => {
        const aVal = getValue(a, sortConfig.key);
        const bVal = getValue(b, sortConfig.key);
        if (aVal < bVal) return sortConfig.direction === "asc" ? -1 : 1;
        if (aVal > bVal) return sortConfig.direction === "asc" ? 1 : -1;
        return 0;
      });
    }
    return sortableData;
  }, [filteredData, sortConfig]);

  const requestSort = (key) => {
    let direction = "asc";
    if (sortConfig.key === key && sortConfig.direction === "asc") {
      direction = "desc";
    }
    setSortConfig({ key, direction });
  };

  const getSortIcon = (key) => {
    if (sortConfig.key !== key) return "";
    return sortConfig.direction === "asc" ? "▲" : "▼";
  };

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value }));
  };

  const resetFilters = () => {
    setFilters({
      marketCapMin: "", marketCapMax: "",
      peRatioMin: "", peRatioMax: "",
      technicalScoreMin: "", technicalScoreMax: "",
      fundamentalScoreMin: "", fundamentalScoreMax: "",
      totalScoreMin: "", totalScoreMax: "",
      companyName: ""
    });
  };

  const toggleRowExpand = (screenerCompanyId) => {
    setExpandedRows(prev => {
      const newSet = new Set(prev);
      if (newSet.has(screenerCompanyId)) {
        newSet.delete(screenerCompanyId);
      } else {
        newSet.add(screenerCompanyId);
      }
      return newSet;
    });
  };

  return (
    <div className="container-fluid mt-2" aria-busy={loadStatus === "loading"}>
      {loadStatus === "loading" && (
        <p className="alert alert-info" role="status">Loading stock data...</p>
      )}
      {loadStatus === "error" && (
        <p className="alert alert-danger" role="alert">
          Unable to load stock data: {loadError}. Try refreshing the page.
        </p>
      )}
      {loadStatus === "loaded" && (
        <>
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
            <h2 className="h5 mb-0">{data.length} Total Stocks <small className="text-body-secondary">({sortedData.length} Filtered Stocks)</small></h2>
            <div className="small text-body-secondary">
              Data Last Refreshed at {generatedAt.toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' })} IST
            </div>
          </div>

          <div className="table-responsive">
            <table className="table table-bordered table-sm align-middle">
        <thead className="table-dark">
          <tr>
            <th colSpan="3">
              <div className="d-grid gap-2">
                <button className="btn btn-sm btn-secondary" onClick={resetFilters}>Reset Filters</button>
              </div>
            </th>
            <th className="text-center">
              <input type="text" className="form-control form-control-sm" name="companyName" value={filters.companyName} onChange={handleFilterChange} placeholder="Filter by name" aria-label="Filter by company name" />
            </th>
            <th className="text-center" >
              <div className="container gx-0">
                <div className="row gx-2">
                  <div className="col"><input type="number" className="form-control form-control-sm" name="marketCapMin" value={filters.marketCapMin} onChange={handleFilterChange} placeholder="Min" aria-label="Minimum market capitalization" /></div>
                  <div className="col"><input type="number" className="form-control form-control-sm" name="marketCapMax" value={filters.marketCapMax} onChange={handleFilterChange} placeholder="Max" aria-label="Maximum market capitalization" /></div>
                </div>
              </div>
            </th>
            <th className="text-center">
              <div className="container gx-0">
                <div className="row gx-2">
                  <div className="col"><input type="number" className="form-control form-control-sm" name="peRatioMin" value={filters.peRatioMin} onChange={handleFilterChange} placeholder="Min" aria-label="Minimum price-to-earnings ratio" /></div>
                    <div className="col"><input type="number" className="form-control form-control-sm" name="peRatioMax" value={filters.peRatioMax} onChange={handleFilterChange} placeholder="Max" aria-label="Maximum price-to-earnings ratio" /></div>
                </div>
              </div>
            </th>
            <th className="text-center">
              <div className="container gx-0">
                <div className="row gx-2">
                  <div className="col"><input type="number" className="form-control form-control-sm" name="fundamentalScoreMin" value={filters.fundamentalScoreMin} onChange={handleFilterChange} placeholder="Min" aria-label="Minimum fundamental score" /></div>
                  <div className="col"><input type="number" className="form-control form-control-sm" name="fundamentalScoreMax" value={filters.fundamentalScoreMax} onChange={handleFilterChange} placeholder="Max" aria-label="Maximum fundamental score" /></div>
                </div>
              </div>
            </th>
            <th className="text-center">
              <div className="container gx-0">
                <div className="row gx-2">
                  <div className="col"><input type="number" className="form-control form-control-sm" name="technicalScoreMin" value={filters.technicalScoreMin} onChange={handleFilterChange} placeholder="Min" aria-label="Minimum technical score" /></div>
                  <div className="col"><input type="number" className="form-control form-control-sm" name="technicalScoreMax" value={filters.technicalScoreMax} onChange={handleFilterChange} placeholder="Max" aria-label="Maximum technical score" /></div>
                </div>
              </div>
            </th>
            <th className="text-center">
              <div className="container gx-0">
                <div className="row gx-2">
                  <div className="col"><input type="number" className="form-control form-control-sm" name="totalScoreMin" value={filters.totalScoreMin} onChange={handleFilterChange} placeholder="Min" aria-label="Minimum total score" /></div>
                  <div className="col"><input type="number" className="form-control form-control-sm" name="totalScoreMax" value={filters.totalScoreMax} onChange={handleFilterChange} placeholder="Max" aria-label="Maximum total score" /></div>
                </div>
              </div>
            </th>
          </tr>
        </thead>
        <thead className="table-dark">
          <tr>
            <th className="text-center align-middle" style={{ width: "40px" }}></th>
            <th className="text-center align-middle" onClick={() => requestSort("screenerCompanyId")}>Screener ID {getSortIcon("screenerCompanyId")}</th>
            <th className="text-center align-middle" onClick={() => requestSort("companyCode")}>Screener Link {getSortIcon("companyCode")}</th>
            <th className="text-center align-middle" onClick={() => requestSort("companyName")}>Name {getSortIcon("companyName")}</th>
            <th className="text-center align-middle" onClick={() => requestSort("marketCap")}>Market Cap (Cr.) {getSortIcon("marketCap")}</th>
            <th className="text-center align-middle" onClick={() => requestSort("peRatio")}>PE Ratio {getSortIcon("peRatio")}</th>
            <th className="text-center align-middle" onClick={() => requestSort("fundamentalScore")}>Fundamental Score {getSortIcon("fundamentalScore")}</th>
            <th className="text-center align-middle" onClick={() => requestSort("technicalScore")}>Technical Score {getSortIcon("technicalScore")}</th>
            <th className="text-center align-middle" onClick={() => requestSort("totalScore")}>Total Score {getSortIcon("totalScore")}</th>
          </tr>
        </thead>
        <tbody>
          {sortedData.map((item) => (
            <React.Fragment key={item.screenerCompanyId}>
              <tr>
                <td className="text-center align-middle" style={{ cursor: "pointer", width: "40px" }}>
                  <button
                    className="btn btn-sm btn-outline-primary"
                    aria-label={`${expandedRows.has(item.screenerCompanyId) ? "Collapse" : "Expand"} details for ${item.companyName}`}
                    aria-expanded={expandedRows.has(item.screenerCompanyId)}
                    onClick={() => toggleRowExpand(item.screenerCompanyId)}
                  >
                    {expandedRows.has(item.screenerCompanyId) ? "−" : "+"}
                  </button>
                </td>
                <td className="text-end">{item.screenerCompanyId}</td>
                <td><a href={item.screenerCompanyLink} target="_blank" rel="noreferrer">{item.companyCode}</a></td>
                <td>{item.companyName}</td>
                <td className="text-end">{item.attributes.marketCap.toLocaleString()}</td>
                <td className="text-end">{item.attributes.peRatio}</td>
                <td className="text-end">{item.companyScore.totalFundamentalScore}</td>
                <td className="text-end">{item.companyScore.totalTechnicalScore}</td>
                <td className="text-end fw-bold">{item.companyScore.totalScore}</td>
              </tr>
              {expandedRows.has(item.screenerCompanyId) && (
                <CompanyDetails companyCode={item.companyCode} />
              )}
            </React.Fragment>
          ))}
        </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}

export default SummaryGrid;
