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
 */
package br.unesp.fc.migracaocontasemail.service;

import br.unesp.fc.migracaocontasemail.service.vo.UsuarioVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CentralService {

    @Value("${central.service}")
    private String serviceUrl;

    private RestTemplate restTemplate;

    @Autowired
    public void createRestTemplate(OAuth2ProtectedResourceDetails resourceDetails) {
        DefaultOAuth2ClientContext clientContext = new DefaultOAuth2ClientContext();
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setAccessTokenUri(resourceDetails.getAccessTokenUri());
        resource.setClientId(resourceDetails.getClientId());
        resource.setClientSecret(resourceDetails.getClientSecret());
        restTemplate = new OAuth2RestTemplate(resource, clientContext);
    }

    public UsuarioVO buscarUsuariosPorIdentificacao(String identificacao) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(serviceUrl)
                .path("/v1/usuarios").queryParam("identificacao", identificacao);
        UsuarioVO[] usuarios = restTemplate.getForObject(
                builder.build().encode().toUri(), UsuarioVO[].class);
        return usuarios != null && usuarios.length > 0
                ? usuarios[0]
                : null;
    }

}
