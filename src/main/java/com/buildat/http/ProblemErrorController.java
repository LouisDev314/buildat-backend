package com.buildat.http;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProblemErrorController implements ErrorController {
    @RequestMapping("/error")
    ResponseEntity<Object> error(HttpServletRequest request) {
        var value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        var status = value instanceof Integer number && number >= 400 && number <= 599 ? number : 500;
        var problem = Problems.create(status, Problems.code(status), Problems.detail(status), request);
        var original = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (original instanceof String path) {
            try {
                problem.setInstance(java.net.URI.create(path.split("\\?", 2)[0]));
            } catch (IllegalArgumentException ignored) {
                problem.setInstance(java.net.URI.create("/"));
            }
        }
        return ResponseEntity.status(status).body(problem);
    }
}
