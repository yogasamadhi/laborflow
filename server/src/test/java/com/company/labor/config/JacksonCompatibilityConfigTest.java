package com.company.labor.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.labor.security.AuthenticationErrorWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

class JacksonCompatibilityConfigTest {
    @Test
    void providesJackson2ObjectMapperForAuthenticationErrorWriter() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(JacksonCompatibilityConfig.class, AuthenticationErrorWriter.class);
            context.refresh();

            assertThat(context.getBean(ObjectMapper.class)).isNotNull();
            AuthenticationErrorWriter writer = context.getBean(AuthenticationErrorWriter.class);
            MockHttpServletResponse response = new MockHttpServletResponse();

            writer.unauthorized(response, "请先登录");

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(response.getContentAsString()).contains("\"message\":\"请先登录\"");
        }
    }
}
