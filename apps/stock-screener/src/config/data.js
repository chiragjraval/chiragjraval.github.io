const defaultDataBaseUrl = import.meta.env.DEV
  ? `${import.meta.env.BASE_URL}screener-data/test/`
  : "/data/stock-screener/prod/";

export const stockScreenerDataBaseUrl = (
  import.meta.env.VITE_STOCK_SCREENER_DATA_BASE_URL || defaultDataBaseUrl
).replace(/\/?$/, "/");
