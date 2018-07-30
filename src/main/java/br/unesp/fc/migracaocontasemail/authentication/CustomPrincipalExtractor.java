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

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.stereotype.Component;

@Component
public class CustomPrincipalExtractor implements PrincipalExtractor {

    private static final Logger log = LoggerFactory.getLogger(CustomPrincipalExtractor.class);

    @Override
    public Object extractPrincipal(Map<String, Object> map) {
        Map<String, Object> details = (Map<String, Object>) map.get("details");
        Map<String, Object> userAuthentication = (Map<String, Object>) map.get("userAuthentication");
        Usuario usuario = new Usuario();
        usuario.setIdUsuario((Integer) details.get("idUsuario"));
        usuario.setUsuario((String) map.get("name"));
        usuario.setNome((String) details.get("nome"));
        usuario.setEmail((String) details.get("email"));
        usuario.setEmailUnesp(usuario.getUsuario() + "@unesp.br");
        usuario.setCpf((String) details.get("cpf"));
        usuario.setRemoteAddress((String) details.get("remoteAddress"));
        usuario.setTokenType((String) details.get("tokenType"));
        usuario.setTokenValue((String) details.get("tokenValue"));
        usuario.setSessionId((String) ((Map<String, Object>) userAuthentication.get("details")).get("sessionId"));
        map.put("usuario", usuario);
        log.info("Login Usuario: {}", usuario.getUsuario());
        return usuario;
    }

}
