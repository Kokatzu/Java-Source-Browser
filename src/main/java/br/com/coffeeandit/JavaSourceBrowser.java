package main.java.br.com.coffeeandit;

import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;
import jdk.jshell.*;
import com.sun.net.httpserver.*;
import static com.sun.net.httpserver.SimpleFileServer.*;

class JavaSourceBrowser {
    private static Path BASE_DIR;

    public static void main(String[] args) throws Exception {
        BASE_DIR = args.length > 0? Paths.get(args[0]) : Paths.get(".");
        BASE_DIR = BASE_DIR.toAbsolutePath();
        var fileHandler = SimpleFileServer.createFileHandler(BASE_DIR);

        var handler = HttpHandlers.handleOrElse(
                JavaSourceBrowser::isJavaSource, JavaSourceBrowser::sendHtmlForJava, fileHandler);

        var output = SimpleFileServer.createOutputFilter(
                System.out, OutputLevel.VERBOSE);

        var port = args.length > 1? Integer.parseInt(args[1]) : 8080;
        var lookback = new InetSocketAddress(port);
        var server = HttpServer.create(lookback, 10, "/", handler, output);
        System.out.printf("visit http://localhost:%d/ from your browser..", port);
        server.start();
    }

    private static boolean isJavaSource(Request r) {
        return r.getRequestURI().toString().endsWith(".java");
    }

    private static void sendHtmlForJava(HttpExchange exchange) {
        try {
            var path = BASE_DIR.resolve(
                    exchange.getRequestURI().toString().substring(1));
            if (!Files.exists(path)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();

            } else {
                exchange.sendResponseHeaders(200, 0);
                var out = new PrintStream(exchange.getResponseBody(), true);
                var src = Files.readString(path);
                out.println(srcToHTML(src));
                exchange.close();
            }
        } catch (IOException exp) {
            exp.printStackTrace();
        }
    }

    private static String srcToHTML(String src) {
        var jshell = JShell.builder().executionEngine("local").build();
        var srcAnalysis = jshell.sourceCodeAnalysis();
        var buf = new StringBuffer();
        buf.append("<html><body><pre><code>");
        int index = 0;
        for (var hl : srcAnalysis.highlights(src)) {
            if (index < hl.start()) {
                buf.append(htmlEncode(src.substring(index, hl.start())));
            }
            buf.append(decorate(hl.attributes(),
                    src.substring(hl.start(), hl.end())));
            index = hl.end();
        }
        buf.append(htmlEncode(src.substring(index)));
        buf.append("</pre></code></body></htm>");
        return buf.toString();
    }

    private static String decorate(Set<SourceCodeAnalysis.Attribute> attrs, String content) {
        var buf = new StringBuilder();
        // start tags
        buf.append(
                attrs.stream().map(attr -> switch(attr) {
                    case DECLARATION -> "<b>";
                    case DEPRECATED -> "<s>";
                    case KEYWORD -> "<font color=\"red\">";
                }).collect(Collectors.joining()));
        // content inside
        buf.append(htmlEncode(content));
        // end tags
        buf.append(
                attrs.stream().map(attr -> switch(attr) {
                    case DECLARATION -> "</b>";
                    case DEPRECATED -> "</s>";
                    case KEYWORD -> "</font>";
                }).collect(Collectors.joining()));
        return buf.toString();
    }

    private static String htmlEncode(String src) {
        var buf = new StringBuilder();
        for (char c : src.toCharArray()) {
            switch (c) {
                case '<' -> buf.append("&lt;");
                case '>' -> buf.append("&gt;");
                case '&' -> buf.append("&amp;");
                case '"' -> buf.append("&quot;");
                default -> buf.append(c);
            }
        }
        return buf.toString();
    }
}
