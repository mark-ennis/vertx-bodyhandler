/*
 * Copyright (C) 2015 eNitiatives.com Pty. Ltd.
 * All rights reserved.
 */
package com.viewds.test.vertx.bodyhandler;

import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.handler.BodyHandler;

public class Example
        implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(Example.class);
    private Vertx vertx;
    
    public static void main(String[] args)
    {
        new Example(args).run();
    }
    
    public static void htmlescape(StringBuilder builder, String value)
    {
        char c;
        int i;

        for (i = 0; i < value.length(); i += 1) {
            c = value.charAt(i);
            switch (c) {
            case '&':
                builder.append("&amp;");
                break;
            case '"':
                builder.append("&quot;");
                break;
            case '<':
                builder.append("&lt;");
                break;
            case '>':
                builder.append("&gt;");
                break;
            default:
                builder.append(c);
                break;
            }
        }
    }

    public Example(String[] args)
    {
    }

    @Override
    public void run()
    {
        Router router;
        
        vertx = Vertx.vertx();
        router = Router.router(vertx);
        /*
         * Set up authentication handler. With this handler present, GET
         * requests to the protected pages will work but POST requests never get
         * a response because body handlers have not been registered at the time
         * the authentication handler is called. For this code to actually work,
         * the body handlers need to be registered before the authentication
         * handler, but it is not obvious to me that this should be the case.
         */
        router.route().handler(this::authentication);
        /*
         * Set up handlers for a restricted resource.
         */
        router.route("/restricted").handler(BodyHandler.create());
        router.route("/restricted").handler(this::restricted);
        /*
         * Set up handlers for another restricted resource.
         */
        router.route("/elsewhere").handler(BodyHandler.create());
        router.route("/elsewhere").handler(this::elsewhere);

        vertx.createHttpServer().requestHandler(router::accept).listen(8080,
                (AsyncResult<HttpServer> event) -> {
                    if (event.succeeded()) {
                        logger.info("started HTTP server successfully");
                    }
                    else {
                        logger.warn("failed to start HTTP server");
                    }
                });
    }

    /**
     * Simulate carrying out asynchronous authentication operation by waiting
     * for a 1 second timer.
     */
    private void authentication(RoutingContext context)
    {
        vertx.setTimer(1000L, (Long timer) -> {
            context.put("authenticated", Boolean.TRUE);
            context.put("id", timer);
            context.next();
        });
    }
    
    /**
     * Present a page protected by the authentication step.
     */
    private void restricted(RoutingContext context)
    {
        final HttpServerRequest request = context.request();
        displayPage("restricted", context.get("authenticated"),
                request, request.params());
    }
    
    /**
     * Present another page protected by the authentication step.
     */
    private void elsewhere(RoutingContext context)
    {
        final HttpServerRequest request = context.request();
        displayPage("elsewhere", context.get("authenticated"),
                request, request.params());
    }

    private void displayPage(String title, Boolean authenticated,
            HttpServerRequest request, MultiMap params)
    {
        String str;
        StringBuilder page;

        page = new StringBuilder("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("<title>").append(title).append(" page</title>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<h3>").append(title).append("</h3>\n");
        page.append("<div>authenticated = ");
        if (authenticated != null) {
            page.append(authenticated);
        }
        else {
            page.append("null");
        }
        page.append("</div>\n");
        page.append("<form method=\"post\">\n");
        
        page.append("<input type=\"text\" name=\"count\" value=\"");
        if (params != null && (str = params.get("count")) != null) {
            page.append(Integer.parseInt(str) + 1);
        }
        else {
            page.append(1);
        }
        page.append("\" readonly>\n<br>\n");
        page.append("<textarea name=\"content\">");
        if (params != null && (str = params.get("content")) != null) {
            htmlescape(page, str);
        }
        page.append("</textarea>\n")
                .append("<input type=\"submit\" name=\"submit\"" +
                        " value=\"submit\">\n")
                .append("</form>\n")
                .append("</body>\n")
                .append("</html>\n");
        request.response()
                .setStatusCode(200)
                .putHeader("Content-type", "text/html; charset=utf-8")
                .end(page.toString());
    }
}
