package com.example.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 – API observability filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter in one class
 * so every HTTP interaction is logged without touching individual resource methods.
 *
 * Why use JAX-RS filters instead of manual Logger.info() in every method?
 * Cross-cutting concerns like logging, authentication, and CORS are orthogonal to
 * business logic. Inserting log statements in every method violates the
 * Single-Responsibility Principle, creates dozens of boilerplate copies that are
 * easy to forget or mis-implement, and makes the resource code harder to read.
 * A filter is declared once with @Provider and automatically applied to every
 * request/response in the entire application. If the logging format ever needs
 * to change, there is exactly one place to update it.
 */
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(ApiLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOG.info(String.format("[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOG.info(String.format("[RESPONSE] %s %s → HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}
