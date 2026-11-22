package com.chirag.stockscreener.context;

import com.chirag.stockscreener.enums.Environment;

public interface ExecutionContext {


    /* Get the current execution environment */
    Environment getEnvironment();

    /* Get the maximum number of pages to scan */
    int getMaxPagesToScan();

    /* Get the screener link to be processed */
    String getScreenerQueryLink();

    /* Get the output directory for scraped data */
    String getOutputDirectory();

}
