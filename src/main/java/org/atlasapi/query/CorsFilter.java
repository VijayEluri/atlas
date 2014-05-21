package org.atlasapi.query;

import javax.servlet.http.HttpServletRequest;

import com.metabroadcast.common.webapp.http.AbstractCorsFilter;


public class CorsFilter extends AbstractCorsFilter {

    @Override
    protected boolean allowCors(String origin, HttpServletRequest request) {
        // This is always allowing cors for every origin: Atlas is protected by the use of an API key in the requests
        return true;
    }

}