package com.buildat.http;

import java.util.UUID;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.*;

class RequestIdFilterTest {
    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void ignoresCallerIdAndDoesNotLeakIntoTheNextRequest() throws Exception {
        var first = new MockHttpServletRequest();
        first.addHeader(RequestIdFilter.HEADER, "caller-controlled");
        var response = new MockHttpServletResponse();
        filter.doFilter(first, response, (request, result) ->
                assertThat(MDC.get("requestId")).isEqualTo(response.getHeader(RequestIdFilter.HEADER)));
        var id = response.getHeader(RequestIdFilter.HEADER);
        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(MDC.get("requestId")).isNull();
        var second = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), second, (request, result) -> { });
        assertThat(second.getHeader(RequestIdFilter.HEADER)).isNotEqualTo(id);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void reusesIdOnErrorDispatchAndCleansMdcAfterExceptions() throws Exception {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/failed");
        var response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new ServletException("failure");
        })).isInstanceOf(ServletException.class);
        var id = response.getHeader(RequestIdFilter.HEADER);
        assertThat(MDC.get("requestId")).isNull();
        request.setDispatcherType(DispatcherType.ERROR);
        request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, "/failed");
        var error = new MockHttpServletResponse();
        filter.doFilter(request, error, (req, res) -> assertThat(MDC.get("requestId")).isEqualTo(id));
        assertThat(error.getHeader(RequestIdFilter.HEADER)).isEqualTo(id);
        assertThat(MDC.get("requestId")).isNull();
    }
}
