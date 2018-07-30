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
package br.unesp.fc.migracaocontasemail.service;

import br.unesp.fc.migracaocontasemail.authentication.Usuario;
import br.unesp.fc.migracaocontasemail.service.RunCommandService.Status;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MigracaoContatoService {

    @Autowired
    private AuthorizationCodeFlow flow;

    @Autowired
    private RunCommandService run;

    @Autowired
    private HttpTransport httpTransport;

    @Autowired
    private JsonFactory jsonFactory;

    public void migrar(Usuario usuario) throws IOException, InterruptedException {
        Credential credential = flow.loadCredential(usuario.getUsuario());
        if (credential == null || credential.getAccessToken() == null) {
            throw new RuntimeException("Erro ao buscar credenciais Google para o usuário " + usuario.getUsuario());
        }
        PeopleService service = new PeopleService.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Migração Contas Email")
                .build();
        for (Usuario.EmailMigracao emailMigracao : usuario.getEmailsValidos()) {
            if (!emailMigracao.isMigrarContatos())
                continue;
            Status status = run.contato(usuario, emailMigracao);
            if (status.getExitValue() != 0) {
                throw new RuntimeException("Erro executando comando de contatos: " + emailMigracao.getEmail());
            }
            InputStream stream = new ByteArrayInputStream(status.getOutput().getBytes(StandardCharsets.UTF_8));
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(new InputStreamReader(stream));
            for (CSVRecord record : records) {
                String email = record.get(0);
                String givenName = record.get(1);
                String familyName = record.get(2);
                String fullName = record.get(3);
                Person person = new Person();
                EmailAddress emailAddress = new EmailAddress();
                emailAddress.setDisplayName(fullName);
                emailAddress.setType("work");
                emailAddress.setValue(email);
                person.setEmailAddresses(Arrays.asList(emailAddress));
                Name name = new Name();
                name.setGivenName(givenName);
                name.setFamilyName(familyName);
                name.setDisplayName(fullName);
                person.setNames(Arrays.asList(name));
                service.people().createContact(person).execute();
            }
            stream.close();
        }
    }

}
