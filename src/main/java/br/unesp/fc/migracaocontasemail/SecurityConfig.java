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
package br.unesp.fc.migracaocontasemail;

import br.unesp.fc.migracaocontasemail.authentication.CustomAuthenticationSuccessHandler;
import br.unesp.fc.migracaocontasemail.authentication.CustomOAuth2ClientAuthenticationProcessingFilter;
import br.unesp.fc.migracaocontasemail.authentication.CustomPrincipalExtractor;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.CompositeFilter;

@Configuration
@EnableOAuth2Client
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    OAuth2ClientContext oauth2ClientContext;

    @Bean
    public FilterRegistrationBean oauth2ClientFilterRegistration(
        OAuth2ClientContextFilter filter) {
      FilterRegistrationBean registration = new FilterRegistrationBean();
      registration.setFilter(filter);
      registration.setOrder(-100);
      return registration;
    }

    @Bean
    @ConfigurationProperties(value = "security.oauth2.client")
    public AuthorizationCodeResourceDetails oauth2Client() {
        return new AuthorizationCodeResourceDetails();
    }

    @Bean
    @ConfigurationProperties(value = "security.oauth2.resource")
    public ResourceServerProperties oauth2Resource() {
        return new ResourceServerProperties();
    }

    @Autowired
    private CustomPrincipalExtractor customPrincipalExtractor;

    private Filter ssoFilter() {
        CompositeFilter filter = new CompositeFilter();
        List<Filter> filters = new ArrayList<>();
        OAuth2ClientAuthenticationProcessingFilter oAuth2Filter = new CustomOAuth2ClientAuthenticationProcessingFilter("/login");
        oAuth2Filter.setAuthenticationSuccessHandler(new CustomAuthenticationSuccessHandler());
        AuthorizationCodeResourceDetails authorizationCodeResourceDetails = oauth2Client();
        OAuth2RestTemplate facebookTemplate = new OAuth2RestTemplate(authorizationCodeResourceDetails, oauth2ClientContext);
        oAuth2Filter.setRestTemplate(facebookTemplate);
        UserInfoTokenServices tokenServices = new UserInfoTokenServices(oauth2Resource().getUserInfoUri(), authorizationCodeResourceDetails.getClientId());
        tokenServices.setPrincipalExtractor(customPrincipalExtractor);
        tokenServices.setRestTemplate(facebookTemplate);
        oAuth2Filter.setTokenServices(tokenServices);
        filters.add(oAuth2Filter);
        filter.setFilters(filters);
        return filter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors()
            .and()
                .authorizeRequests()
                    .antMatchers("/webjars/**").permitAll()
                    .antMatchers("/layout/**").permitAll()
                    .antMatchers("/").permitAll()
                    .antMatchers("/*.ico", "/*.png").permitAll()
                    .antMatchers("/invalido").authenticated()
            .and()
                .logout()
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                    .logoutSuccessUrl("/")
            .and()
                .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
        ;
    }

}
