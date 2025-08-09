package com.shoppingcart.multitenant.interceptor;

import com.shoppingcart.multitenant.config.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = extractTenantId(request);
        
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = "default";
        }
        
        logger.debug("Setting tenant context to: {}", tenantId);
        TenantContext.setCurrentTenant(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        TenantContext.clear();
    }

    private String extractTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null) {
            return tenantId;
        }

        String subdomain = extractSubdomain(request);
        if (subdomain != null && !subdomain.equals("www")) {
            return subdomain;
        }

        String pathTenant = extractFromPath(request);
        if (pathTenant != null) {
            return pathTenant;
        }

        return "default";
    }

    private String extractSubdomain(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName != null && serverName.contains(".")) {
            String[] parts = serverName.split("\\.");
            if (parts.length > 2) {
                return parts[0];
            }
        }
        return null;
    }

    private String extractFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/tenant/")) {
            String[] pathParts = path.split("/");
            if (pathParts.length > 2) {
                return pathParts[2];
            }
        }
        return null;
    }
}