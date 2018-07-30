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
import br.unesp.fc.migracaocontasemail.data.Migracao;
import br.unesp.fc.migracaocontasemail.data.MigracaoExec;
import br.unesp.fc.migracaocontasemail.service.RunCommandService.Status;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MigracaoEmailService {

    private static final Logger log = LoggerFactory.getLogger(MigracaoEmailService.class);

    private static final long fixedDelay = 30 * 60 * 1000L; // 30 minutos;
    private static final long initialDelay = 1 * 60 * 1000L; // 1 minutos;

    @Autowired
    private RunCommandService run;

    @Autowired
    private DataService data;

    @Autowired
    private SendEmailService sendEmail;

    private final Set<Migracao> migracaoSet = ConcurrentHashMap.newKeySet();

    @Scheduled(initialDelay = initialDelay, fixedDelay = fixedDelay)
    public void initMigracoes() {
        log.trace("Iniciando migrações");
        migracaoSet.removeIf(Migracao::isCompletado);
        List<Migracao> lista = data.listarMigracaoPendentes();
        for (Migracao migracao : lista) {
            initMigracao(migracao);
        }
    }

    public void initMigracao(Migracao migracao) {
        if (!migracaoSet.add(migracao)) {
            return;
        }
        Task task = new Task(migracao);
        Thread thread = new Thread(task, migracao.getEmail().getUsuario().getUsuario()
                + ":" + migracao.getEmail().getEmail());
        thread.start();
    }

    public void ativarForward(Usuario usuario) throws IOException, InterruptedException {
        for (Usuario.EmailMigracao emailMigracao : usuario.getEmailsValidos()) {
            run.forward(usuario, emailMigracao);
        }
    }

    public class Task implements Runnable {

        public final Migracao migracao;

        public Task(Migracao migracao) {
            this.migracao = migracao;
        }

        @Override
        public void run() {
            try {
                MDC.put("user", migracao.getEmail().getUsuario().getUsuario());
                migrar();
                migracao.setCompletado(true);
                data.salvar(migracao);
                try {
                    sendEmail.sendNotificacao(migracao.getEmail().getUsuario().getUsuario(), migracao.getEmail());
                } catch (MessagingException | IOException ex) {
                    log.error("Erro enviando mensagem de encerramento: {}", migracao.getEmail().getEmail(), ex);
                }
            } finally {
                MDC.clear();
            }
        }

        public void migrar() {
            boolean sucesso = false;
            for (int i = 0; i < 5 && !sucesso; i++) {
                try {
                    MigracaoExec migracaoExec = new MigracaoExec();
                    migracaoExec.setMigracao(migracao);
                    migracaoExec.setInicio(LocalDateTime.now());
                    migracaoExec = data.salvar(migracaoExec);
                    Status status = run.migrarEmails(migracao);
                    sucesso = status.getExitValue() == 0;
                    log.info("Migracao {} Status: {}", migracao.getEmail().getEmail(),
                            status.getExitValue());
                    migracaoExec.setTermino(LocalDateTime.now());
                    migracaoExec.setStatus(status.getExitValue());
                    migracaoExec = data.salvar(migracaoExec);
                } catch (Exception ex) {
                    log.error("Erro migrando os emails", ex);
                }
                if (!sucesso) {
                    try {
                        Thread.sleep(fixedDelay);
                    } catch (InterruptedException ex) {
                        log.error("", ex);
                    }
                }
            }
            if (sucesso) {
                log.info("Migração efetuada {}", migracao.getEmail().getEmail());
            } else {
                log.error("Migração não realizada com sucesso {}", migracao.getEmail().getEmail());
            }
        }

    }

}
