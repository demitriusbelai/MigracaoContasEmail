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

import br.unesp.fc.migracaocontasemail.data.Email;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendEmailService {

    private static final Logger log = LoggerFactory.getLogger(SendEmailService.class);

    @Autowired
    private UnidadeConfigService unidadeConfig;

    @Autowired
    private AuthorizationCodeFlow flow;

    @Autowired
    private HttpTransport httpTransport;

    @Autowired
    private JsonFactory jsonFactory;

    @Value("${configDir}")
    private String configDir;

    private Session session;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        session = Session.getDefaultInstance(props, null);
    }

    public void sendNotificacao(String usuario, Email email) throws MessagingException, FileNotFoundException, IOException {
        String emailFileName = unidadeConfig.getEmailNotificacao(email.getDominio());
        FileInputStream fis = new FileInputStream(Paths.get(configDir).resolve(emailFileName).toFile());
        MimeMessage message = new MimeMessage(session, fis);
        fis.close();
        String emailUnesp = usuario + "@unesp.br";
        message.setRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(emailUnesp));
        message.setSentDate(new Date());
        Map<String, String> values = new HashMap<>();
        values.put("email", email.getEmail());
        values.put("emailUnesp", emailUnesp);
        replaceMessage(message, values);
        Credential credential = flow.loadCredential(usuario);
        if (credential == null || credential.getAccessToken() == null
                || credential.getRefreshToken() == null) {
            throw new RuntimeException("Erro ao buscar credenciais Google para o usuário " + usuario);
        }
        Gmail service = new Gmail.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Migração Contas Email")
                .build();
        insertMessage(service, "me", message);
    }

    private static Message insertMessage(Gmail service, String userId, MimeMessage email)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(email);
        message = service.users().messages().insert(userId, message).execute();
        ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(Collections.singletonList("UNREAD"));
        service.users().messages().modify("me", message.getId(), mods).execute();
        return message;
    }

    private static Message createMessageWithEmail(MimeMessage email)
            throws MessagingException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        email.writeTo(baos);
        String encodedEmail = Base64.encodeBase64URLSafeString(baos.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private String replace(String str, Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            str = str.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return str;
    }

    private void replaceMessage(MimeMessage message, Map<String, String> values) throws IOException, MessagingException {
        Object content = message.getContent();
        if (content instanceof MimeMultipart) {
            Multipart multiPart = (Multipart) content;
            for (int j = 0; j < multiPart.getCount(); j++) {
                BodyPart part = multiPart.getBodyPart(j);
                Object partContent = part.getContent();
                if (partContent instanceof String) {
                    String str = (String) partContent;
                    str = replace(str, values);
                    part.setContent(str, part.getContentType());
                }
            }
        } else if (content instanceof String) {
            message.setContent(replace((String) content, values), message.getContentType());
        }
        message.saveChanges();
    }

}
