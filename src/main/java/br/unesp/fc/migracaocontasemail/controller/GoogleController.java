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
package br.unesp.fc.migracaocontasemail.controller;

import br.unesp.fc.migracaocontasemail.authentication.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/google")
public class GoogleController {

    private static final Logger log = LoggerFactory.getLogger(GoogleController.class);

    @Autowired
    private AuthorizationCodeFlow flow;

    @RequestMapping("/login")
    public String login(HttpServletRequest req, HttpServletResponse resp, Model model, Usuario usuario) throws IOException {
        // load credential from persistence store
        Credential credential = flow.loadCredential(usuario.getUsuario());
        // if credential found with an access token, invoke the user code
        if (credential != null && credential.getAccessToken() != null
                && credential.getRefreshToken() != null) {
            Map<String, String> user = getGoogleUser(credential);
            // Verifica se é uma conta da Unesp
            if (!Boolean.valueOf(user.get("email_verified"))
                    || !user.get("email").equals(usuario.getEmailUnesp())) {
                log.warn("Conta inválida: {}", user.get("email"));
                flow.getCredentialDataStore().delete(usuario.getUsuario());
                model.addAttribute("error", "Você deve entrar na Google com sua conta G Suite da Unesp!");
                return "migrar/confirmar";
            }
            usuario.hasGoogleCredential(true);
            return "redirect:/migrar/iniciar";
        }
        String state = new BigInteger(130, new SecureRandom()).toString(32);
        req.getSession().setAttribute("googleState", state);
        // redirect to the authorization flow
        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
        authorizationUrl.setRedirectUri(getRedirectUri(req));
        authorizationUrl.setState(state);
        authorizationUrl.set("login_hint", usuario.getUsuario() + "@unesp.br");
        authorizationUrl.set("hd", "unesp.br");
        return "redirect:" + authorizationUrl.build();
    }

    @RequestMapping("/callback")
    public String callback(HttpServletRequest req, HttpServletResponse resp,
            Usuario usuario, Authentication authentication, Model model) throws IOException {
        StringBuffer buf = req.getRequestURL();
        if (req.getQueryString() != null) {
            buf.append('?').append(req.getQueryString());
        }
        AuthorizationCodeResponseUrl responseUrl = new AuthorizationCodeResponseUrl(buf.toString());
        String code = responseUrl.getCode();
        String state = responseUrl.getState();
        if (responseUrl.getError() != null) {
            if (responseUrl.getError().equals("access_denied")) {
                log.warn("Não autorizou acesso à conta Google");
                model.addAttribute("error", "Você deve autorizar esta aplicação a ter acesso a sua conta Google!");
                return "migrar/confirmar";
            }
            throw new ValidationException(responseUrl.getError());
        } else if (code == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("Missing authorization code");
            return null;
        } else if (state == null || !state.equals(req.getSession().getAttribute("googleState"))) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("Missing or invalid state");
            return null;
        } else {
            String redirectUri = getRedirectUri(req);
            TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
            Credential credential = flow.createAndStoreCredential(response, usuario.getUsuario());
            if (credential.getRefreshToken() == null) {
                flow.getCredentialDataStore().delete(usuario.getUsuario());
                model.addAttribute("error", "A google não nos deu um token do tipo off-line!");
                return "migrar/confirmar";
            }
            Map<String, String> user = getGoogleUser(credential);
            // Verifica se é uma conta da Unesp
            if (!Boolean.valueOf(user.get("email_verified"))
                    || !user.get("email").equals(usuario.getEmailUnesp())) {
                log.warn("Conta inválida: {}", user.get("email"));
                flow.getCredentialDataStore().delete(usuario.getUsuario());
                model.addAttribute("error", "Você deve entrar na Google com sua conta G Suite da Unesp!");
                return "migrar/confirmar";
            }
            usuario.hasGoogleCredential(true);
            return "redirect:/migrar/iniciar";
        }
    }

    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/plus/v1/people/me/openIdConnect";

    @Autowired
    private HttpTransport httpTransport;

    private Map<String, String> getGoogleUser(Credential credential) throws IOException {
        final HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
        final GenericUrl url = new GenericUrl(USERINFO_ENDPOINT);      // Make an authenticated request.
        final HttpRequest request = requestFactory.buildGetRequest(url);
        request.getHeaders().setContentType("application/json");

        final String jsonIdentity = request.execute().parseAsString();
        HashMap<String, String> userIdResult
                = new ObjectMapper().readValue(jsonIdentity, HashMap.class);
        return userIdResult;
    }

    private static String getRedirectUri(HttpServletRequest req) {
        GenericUrl url = new GenericUrl(req.getRequestURL().toString());
        url.setRawPath(req.getContextPath() + "/google/callback");
        return url.build();
    }

}
