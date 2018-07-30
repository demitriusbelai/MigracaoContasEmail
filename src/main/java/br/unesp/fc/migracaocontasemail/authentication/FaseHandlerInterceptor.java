/*
 * MIT License
 *
 * Copyright (c) 2018 Demitrius Belai
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package br.unesp.fc.migracaocontasemail.authentication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class FaseHandlerInterceptor implements HandlerInterceptor {

    Logger log = LoggerFactory.getLogger(FaseHandlerInterceptor.class);

    @FunctionalInterface
    public interface CheckUser {
        boolean check(Usuario usuario);
    }

    List<CheckUser> fases = Arrays.asList(
            u -> u != null,
            u -> !u.getEmailsValidos().isEmpty(),
            u -> !u.getEmailsValidos().stream().filter(e -> !e.isSenhaValidada()).findAny().isPresent(),
            u -> u.hasGoogleCredential()
    );

    // RequestMatcher -> Fase
    Map<RequestMatcher, Integer> mappers = new HashMap<>();

    // Fase -> URL Redirecionamento
    Map<Integer, String> urlMapper = new HashMap<>();

    public FaseHandlerInterceptor() {
        mappers.put(new AntPathRequestMatcher("/migrar/buscardados"), 1);
        mappers.put(new AntPathRequestMatcher("/migrar/dadoscentral"), 2);
        mappers.put(new AntPathRequestMatcher("/migrar/verificaremail"), 2);
        mappers.put(new AntPathRequestMatcher("/migrar/confirmar"), 3);
        mappers.put(new AntPathRequestMatcher("/google/**"), 3);
        mappers.put(new AntPathRequestMatcher("/migrar/iniciar"), 4);
        mappers.put(new AntPathRequestMatcher("/migrar/executar"), 4);
        urlMapper.put(1, "/");
        urlMapper.put(2, "/invalido");
        urlMapper.put(3, "/dadoscentral");
        urlMapper.put(4, "/migrar/confirmar");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = (Authentication) request.getUserPrincipal();
        Usuario usuario = auth != null ? (Usuario) auth.getPrincipal() : null;
        int verificar = 0;
        for (Map.Entry<RequestMatcher, Integer> m : mappers.entrySet()) {
            if (m.getKey().matches(request)) {
                for (; verificar < m.getValue(); verificar++) {
                    if (!fases.get(verificar).check(usuario)) {
                        String url = urlMapper.get(m.getValue());
                        response.sendRedirect(request.getContextPath() + url);
                        return false;
                    }
                }
            }
        }
        if (verificar > 0) {
            log.info("Request: {}", request.getRequestURL());
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

}
