package br.com.jumplab.api.core.filtros;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import br.com.jumplab.api.core.exceptions.CorpoGrandeDemaisException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Limite de 2 MB no corpo de {@code /api/sync/**} (spec 013). Com Content-Length declarado, recusa
 * antes de ler; sem ele (envio em partes), conta os bytes lidos e para no limite.
 */
@Component
public class LimiteCorpoSincronizacaoFilter extends OncePerRequestFilter {

    public static final long LIMITE_BYTES = 2L * 1024 * 1024;
    private static final String PREFIXO = "/api/sync/";

    private final ObjectMapper json;

    public LimiteCorpoSincronizacaoFilter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIXO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadeia)
        throws ServletException, IOException {
        if (request.getContentLengthLong() > LIMITE_BYTES) {
            response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            json.writeValue(response.getOutputStream(), ApiResponse.erro(CorpoGrandeDemaisException.MENSAGEM));
            return;
        }
        cadeia.doFilter(new RequisicaoLimitada(request), response);
    }

    private static final class RequisicaoLimitada extends HttpServletRequestWrapper {

        RequisicaoLimitada(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream original = super.getInputStream();
            return new ServletInputStream() {
                private long lidos;

                @Override
                public int read() throws IOException {
                    int b = original.read();
                    if (b >= 0 && ++lidos > LIMITE_BYTES) throw new CorpoGrandeDemaisException();
                    return b;
                }

                @Override
                public int read(byte[] destino, int inicio, int tamanho) throws IOException {
                    int n = original.read(destino, inicio, tamanho);
                    if (n > 0 && (lidos += n) > LIMITE_BYTES) throw new CorpoGrandeDemaisException();
                    return n;
                }

                @Override
                public boolean isFinished() {
                    return original.isFinished();
                }

                @Override
                public boolean isReady() {
                    return original.isReady();
                }

                @Override
                public void setReadListener(ReadListener ouvinte) {
                    original.setReadListener(ouvinte);
                }
            };
        }
    }
}
