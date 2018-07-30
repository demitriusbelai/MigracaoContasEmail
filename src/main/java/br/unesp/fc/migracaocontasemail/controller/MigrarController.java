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
import br.unesp.fc.migracaocontasemail.authentication.Usuario.EmailMigracao;
import br.unesp.fc.migracaocontasemail.data.Email;
import br.unesp.fc.migracaocontasemail.data.Migracao;
import br.unesp.fc.migracaocontasemail.service.CentralService;
import br.unesp.fc.migracaocontasemail.service.DataService;
import br.unesp.fc.migracaocontasemail.service.ImapService;
import br.unesp.fc.migracaocontasemail.service.MigracaoContatoService;
import br.unesp.fc.migracaocontasemail.service.MigracaoEmailService;
import br.unesp.fc.migracaocontasemail.service.RunCommandService;
import br.unesp.fc.migracaocontasemail.service.UnidadeConfigService;
import br.unesp.fc.migracaocontasemail.service.vo.UsuarioVO;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.WebUtils;

@Controller
@RequestMapping("/migrar")
public class MigrarController {

    private static final Logger log = LoggerFactory.getLogger(MigrarController.class);

    @Autowired
    private UnidadeConfigService unidadeConfig;

    @Autowired
    private ImapService imapService;

    @Autowired
    private RunCommandService runCommand;

    @Autowired
    private CentralService central;

    @Autowired
    private MigracaoContatoService contatoService;

    @Autowired
    private MigracaoEmailService emailService;

    @Autowired
    private DataService dataService;

    private final Pattern reEmail = Pattern.compile("[a-z_.]+@([a-z.]+)");

    @RequestMapping("")
    public String root() {
        // Página de loading - Redirect /buscardados
        return "migrar/root.html";
    }

    @RequestMapping("/buscardados")
    public String buscarDados(Usuario usuario) {
        UsuarioVO usuarioVO = central.buscarUsuariosPorIdentificacao(
                usuario.getUsuario());

        String emails = usuarioVO.getEmail();
        log.info("Emails para migração: {}", emails);

        for (String email: emails.split(",")) {

            email = email.trim();
            Matcher m = reEmail.matcher(email);

            if (m.matches()) {
                Usuario.EmailMigracao emailMigracao = new Usuario.EmailMigracao(email, m.group(1));
                usuario.addEmail(emailMigracao);
                if (!unidadeConfig.contem(emailMigracao.getDominio())) {
                    emailMigracao.setErro("Email sem suporte a migração");
                    continue;
                }
                RunCommandService.Status status;
                try {
                    status = runCommand.validarEmail(usuario, emailMigracao);
                    if (status.getExitValue() != 0) {
                        emailMigracao.setErro(status.getOutput());
                        log.warn("Email inválido {}: {}", email, emailMigracao.getErro());
                    }
                    List<Email> listaEmail = dataService.buscarEmail(usuario.getUsuario(), email);
                    Optional<Email> opEmail = listaEmail.stream().filter(e -> e.getErro() == null).findFirst();
                    if (opEmail.isPresent()) {
                        List<Migracao> lista = dataService.buscarMigracaoPorEmail(email);
                        Optional<Migracao> opMigracao = lista.stream().filter(me -> me.isCompletado()).findFirst();
                        if (!opEmail.get().isMigrarEmails() || opMigracao.isPresent()) {
                            emailMigracao.setErro("Email já migrado");
                        } else {
                            emailMigracao.setErro("Email em migração");
                        }
                    }
                } catch (Exception ex) {
                    log.error("Erro validando email: {}", email, ex);
                    // Apenas para não deixar o usuário passar
                    emailMigracao.setErro(ex.getMessage());
                }
            }
        }

        if (usuario.getEmailsValidos().size() > 0) {
            return "redirect:/migrar/dadoscentral";
        }

        return "redirect:/invalido";
    }

    public static class DadosCentralEmail {
        private String email;
        private String senha;
        private String erro;
        private Boolean migrarEmail;
        private Boolean migrarContato;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }

        public String getErro() {
            return erro;
        }

        public void setErro(String erro) {
            this.erro = erro;
        }

        public Boolean getMigrarEmail() {
            return migrarEmail;
        }

        public void setMigrarEmail(Boolean migrarEmail) {
            this.migrarEmail = migrarEmail;
        }

        public Boolean getMigrarContato() {
            return migrarContato;
        }

        public void setMigrarContato(Boolean migrarContato) {
            this.migrarContato = migrarContato;
        }

    }

    public static class DadosCentralForm {
        private List<DadosCentralEmail> emails = new ArrayList<>();

        public List<DadosCentralEmail> getEmails() {
            return emails;
        }

        public void setEmails(List<DadosCentralEmail> emails) {
            this.emails = emails;
        }

    }

    @RequestMapping("/dadoscentral")
    public String dadosCentral(Usuario usuario, Model model) {
        DadosCentralForm form = new DadosCentralForm();
        for (EmailMigracao migracao : usuario.getEmails()) {
            DadosCentralEmail email = new DadosCentralEmail();
            email.setEmail(migracao.getEmail());
            email.setErro(migracao.getErro());
            email.setMigrarEmail(Boolean.TRUE);
            email.setMigrarContato(Boolean.TRUE);
            form.getEmails().add(email);
        }
        model.addAttribute("form", form);
        return "migrar/dadoscentral";
    }

    @RequestMapping("/verificaremail")
    public String verificarEmail(Usuario usuario, @RequestParam Map<String, String> parameters,
            @ModelAttribute("form") DadosCentralForm form, Model model) {
        DirectFieldBindingResult result = new DirectFieldBindingResult(form, "form");
        for (int i = 0; i < form.getEmails().size(); i++) {
            DadosCentralEmail email = form.getEmails().get(i);
            Usuario.EmailMigracao emailMigracao = usuario.getEmails().get(i);
            email.setEmail(emailMigracao.getEmail());
            email.setErro(emailMigracao.getErro());
            if (emailMigracao.getErro() != null) {
                continue;
            }
            String servidor = unidadeConfig.getServidor(emailMigracao.getDominio());
            String senha = email.getSenha();
            if (!imapService.checkUserPassword(servidor, emailMigracao.getEmail(), senha)) {
                result.addError(new FieldError(result.getObjectName(), "emails[" + i + "].senha", "Senha inválida!"));
            } else {
                emailMigracao.setSenhaValidada(true);
                emailMigracao.setMigrarEmails(email.getMigrarEmail());
                emailMigracao.setMigrarContatos(email.getMigrarContato());
            }
        }
        if (!result.getAllErrors().isEmpty()) {
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + result.getObjectName(), result);
            return "migrar/dadoscentral";
        }
        return "redirect:/migrar/confirmar";
    }

    @RequestMapping("/confirmar")
    public String confirmar() {
        log.info("Confirmar dados");
        return "migrar/confirmar";
    }

    @RequestMapping("/iniciar")
    public String iniciar() {
        return "migrar/iniciar";
    }

    @RequestMapping("/executar")
    public String executar(Usuario usuario, HttpServletRequest req) throws IOException, InterruptedException {
        HttpSession session = req.getSession();
        Object mutex = WebUtils.getSessionMutex(session);
        if (session.getAttribute("iniciado") != null) {
            return "redirect:/migrar/termino";
        }
        synchronized (mutex) {
            if (session.getAttribute("iniciado") != null) {
                return "redirect:/migrar/termino";
            }
            br.unesp.fc.migracaocontasemail.data.Usuario temp;
            br.unesp.fc.migracaocontasemail.data.Usuario u;
            temp = dataService.buscarUsuario(usuario.getUsuario());
            if (temp == null) {
                u = new br.unesp.fc.migracaocontasemail.data.Usuario();
            } else {
                u = temp;
            }
            List<Email> listaEmail = usuario.getEmails().stream().map(e -> {
                Email email = new Email();
                email.setEmail(e.getEmail());
                email.setUsuario(u);
                email.setDominio(e.getDominio());
                email.setMigrarEmails(e.isMigrarEmails());
                email.setMigrarContatos(e.isMigrarContatos());
                if (e.getErro() != null) {
                    email.setErro(e.getErro());
                }
                return email;
            }).collect(Collectors.toList());
            u.setUsuario(usuario.getUsuario());
            u.setEmails(listaEmail);
            dataService.salvar(u);
            contatoService.migrar(usuario);
            emailService.ativarForward(usuario);

            List<Migracao> listaMigracao = new ArrayList<>();
            for (Email email : listaEmail) {
                if (email.isMigrarEmails()) {
                    Migracao migracao = new Migracao();
                    migracao.setEmail(email);
                    migracao.setHorario(LocalDateTime.now());
                    listaMigracao.add(migracao);
                }
            }
            listaMigracao = dataService.salvarMigracao(listaMigracao);
            for (Migracao migracao : listaMigracao) {
                emailService.initMigracao(migracao);
            }
            session.setAttribute("iniciado", true);
            return "redirect:/migrar/termino";
        }
    }

    @RequestMapping("/termino")
    public String termino(Usuario usuario) throws IOException, InterruptedException {
        return "migrar/termino";
    }

}
