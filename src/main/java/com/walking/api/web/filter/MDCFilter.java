package com.walking.api.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.RequestFacade;
import org.apache.commons.lang3.time.StopWatch;
import org.jboss.logging.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MDCFilter implements Filter {

	private final ObjectMapper objectMapper;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Object traceId = UUID.randomUUID().toString();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		MDC.put("traceId", traceId);
		MDC.put("referer", ((RequestFacade) request).getHeader("referer"));
		MDC.put("userAgent", ((RequestFacade) request).getHeader("user-agent"));
		MDC.put("requestAddr", request.getRemoteAddr());
		MDC.put("requestURL", ((RequestFacade) request).getRequestURL());
		MDC.put("requestMethod", ((RequestFacade) request).getMethod());
		log.info(
				"[{}] {} -> [{}] uri : {}",
				traceId,
				request.getRemoteAddr(),
				((RequestFacade) request).getMethod(),
				((RequestFacade) request).getRequestURL());
		chain.doFilter(request, response);
		stopWatch.stop();
		MDC.put("elapsedTime", stopWatch.getTime());
		log.info("request-log: {}", objectMapper.writeValueAsString(MDC.getMap()));
		MDC.clear();
	}
}
