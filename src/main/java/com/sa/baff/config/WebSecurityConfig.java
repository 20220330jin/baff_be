package com.sa.baff.config;

import com.sa.baff.handler.OAuth2SuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;

@Configurable
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final DefaultOAuth2UserService oAuth2UserService;
      private final OAuth2SuccessHandler oAuth2SuccessHandler;
      // JWT 인증을 처리하기 위해 직접 구현한 필터를 의존성으로 주입받습니다.
      private final JwtAuthenticationFilter jwtAuthenticationFilter;

      @Bean
      protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {
          httpSecurity
                  // CORS(Cross-Origin Resource Sharing) 설정을 활성화합니다. (이 부분은 기존 설정을 유지합니다)
                  .cors(cors -> cors
                          .configurationSource(configurationSource()))
                  // CSRF(Cross-Site Request Forgery) 보호 기능을 비활성화합니다. JWT를 사용하는 경우 일반적으로 비활성화합니다.
                  .csrf(CsrfConfigurer::disable)
                  // HTTP Basic 인증을 비활성화합니다.
                  .httpBasic(HttpBasicConfigurer::disable)
                  // 세션을 상태 없이(stateless) 관리하도록 설정합니다. JWT 기반 인증에서는 서버가 세션을 유지할 필요가 없습니다.
                  .sessionManagement(sessionManagement -> sessionManagement
                          .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                  /**
                   * HTTP 요청에 대한 접근 권한을 설정합니다.
                   * antMatchers 대신 requestMatchers를 사용합니다.
                   */
                  .authorizeHttpRequests(authorize -> authorize
                          // '/api/user/me' 경로는 모든 사용자(인증 여부와 상관없이) 접근을 허용합니다. (내 정보 조회)
//                          .requestMatchers("/api/user/me").permitAll()
                          // '/api/question/createquestion' 경로는 인증된 사용자만 접근을 허용합니다.
//                          .requestMatchers("/api/question/createquestion").authenticated()
                          // '/api/comment/createComment' 경로는 모든 사용자(회원, 비회원)의 접근을 허용합니다.
//                          .requestMatchers("/api/comment/createComment").permitAll()
                          // 위에서 지정한 경로 외의 모든 요청은 일단 허용합니다.
                          .anyRequest().permitAll()
                  )
                  /**
                   * OAuth2 소셜 로그인을 설정합니다.
                   * 로그인 성공 시의 처리와 사용자 정보 로드 방법을 지정합니다.
                   */
                  .oauth2Login(oauth2 -> oauth2
                          .userInfoEndpoint(endpoint -> endpoint
                                  .userService(oAuth2UserService))
                          .successHandler(oAuth2SuccessHandler))
                  /**
                   * 인증 과정에서 발생하는 예외를 처리할 핸들러를 설정합니다.
                   * 여기서는 인증 실패 시 'FailedAuthenticationEntryPoint'가 동작합니다.
                   */
                  .exceptionHandling(exceptionHandling -> exceptionHandling
                          .authenticationEntryPoint(new FailedAuthenticationEntryPoint()))
                  /**

                   * 가장 중요한 부분입니다.
                   * 직접 구현한 JwtAuthenticationFilter를 Spring Security의 기본 필터인 UsernamePasswordAuthenticationFilter 앞에 추가합니다.
                   * 이렇게 함으로써, 모든 API 요청이 컨트롤러에 도달하기 전에 이 필터를 거치게 되어 JWT 유효성 검사가 먼저 수행됩니다.
                   */
                  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

          return httpSecurity.build();
      }

      // CORS 설정
      @Bean
      protected CorsConfigurationSource configurationSource(){
          CorsConfiguration corsConfiguration = new CorsConfiguration();
          corsConfiguration.addAllowedHeader("*");
          corsConfiguration.addAllowedMethod("*");
          corsConfiguration.setAllowCredentials(true);
          corsConfiguration.addAllowedOrigin("http://localhost:5173");
          corsConfiguration.addAllowedOrigin("https://baff-fe.vercel.app");

          UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", corsConfiguration);

          return source;
      }
  }
  /**
   * 실패시 반환 코드 작업
   */
  class FailedAuthenticationEntryPoint implements AuthenticationEntryPoint {

      @Override
      public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
          response.setContentType("application/json");
          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
          // {"code": "NP", "message": "No Permission"}
          response.getWriter().write("{\"code\": \"NP\", \"message\": \"No Permission\"}");
      }
}
