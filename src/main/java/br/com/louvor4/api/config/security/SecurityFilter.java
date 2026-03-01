package br.com.louvor4.api.config.security;

import br.com.louvor4.api.services.UserDetailsServiceImpl;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityFilter(TokenService tokenService, UserDetailsServiceImpl userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            var token = recoverToken(request);

            if (token != null) {
                var username = tokenService.validateToken(token);
                var user = userDetailsService.loadUserByUsername(username);

                var authentication = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);

        } catch (TokenExpiredException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Token expirado\"}");
        } catch (JWTVerificationException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Token inválido\"}");
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}