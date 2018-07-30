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
import br.unesp.fc.migracaocontasemail.authentication.Usuario.EmailMigracao;
import br.unesp.fc.migracaocontasemail.data.Migracao;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RunCommandService {

    private static final Logger log = LoggerFactory.getLogger(RunCommandService.class);

    @Value("${usersDirs}")
    private String usersDirs;

    @Value("${commandsDir}")
    private String commandsDir;

    @Autowired
    private UnidadeConfigService unidadeConfig;

    private int run(Path dir, String... args) throws IOException, InterruptedException {
        log.info("Running {}", Arrays.toString(args));
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(dir.resolve(Paths.get(args[0]).getFileName() + ".out").toFile());
        Process p = pb.start();
        p.waitFor();
        log.info("Run {} Status {}", Arrays.toString(args), p.exitValue());
        return p.exitValue();
    }

    private int run(Path dir, StringBuilder sb, String... args) throws IOException, InterruptedException {
        log.info("Running {}", Arrays.toString(args));
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        char[] buffer = new char[256];
        int len = 0;
        while ((len = reader.read(buffer)) > 0) {
            sb.append(buffer, 0, len);
        }
        log.info("Run {} Status {}", Arrays.toString(args), p.exitValue());
        return p.exitValue();
    }

    private Path getDirUsuarioEmail(Usuario usuario, EmailMigracao emailMigracao) throws IOException {
        Path dir = Paths.get(usersDirs, usuario.getUsuario(), emailMigracao.getEmail());
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private Path getDirUsuarioEmail(Migracao migracao) throws IOException {
        Path dir = Paths.get(usersDirs, migracao.getEmail().getUsuario().getUsuario(), migracao.getEmail().getEmail());
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    public Status validarEmail(Usuario usuario, EmailMigracao emailMigracao) throws IOException, InterruptedException {
        String comandoValidar = unidadeConfig.getComandoValidar(emailMigracao.getDominio());
        String fullcmd = Paths.get(commandsDir, comandoValidar).toString();
        StringBuilder sb = new StringBuilder();
        Path dir = getDirUsuarioEmail(usuario, emailMigracao);
        int status = run(dir, sb, fullcmd, emailMigracao.getEmail());
        return new Status(status, sb.toString());
    }

    public Status contato(Usuario usuario, EmailMigracao emailMigracao) throws IOException, InterruptedException {
        String comandoContato = unidadeConfig.getComandoContato(emailMigracao.getDominio());
        String fullcmd = Paths.get(commandsDir, comandoContato).toString();
        StringBuilder sb = new StringBuilder();
        Path dir = getDirUsuarioEmail(usuario, emailMigracao);
        int status = run(dir, sb, fullcmd, emailMigracao.getEmail());
        return new Status(status, sb.toString());
    }

    public void forward(Usuario usuario, EmailMigracao emailMigracao) throws IOException, InterruptedException {
        String comandoForward = unidadeConfig.getComandoForward(emailMigracao.getDominio());
        String fullcmd = Paths.get(commandsDir, comandoForward).toString();
        Path dir = getDirUsuarioEmail(usuario, emailMigracao);
        if (run(dir, fullcmd, emailMigracao.getEmail(), usuario.getUsuario() + "@unesp.br") != 0) {
            throw new RuntimeException("Erro ativando forward: " + emailMigracao.getEmail());
        }
    }

    public Status migrarEmails(Migracao migracao) throws IOException, InterruptedException {
        String comandoValidar = unidadeConfig.getComandoMigrar(migracao.getEmail().getDominio());
        String fullcmd = Paths.get(commandsDir, comandoValidar).toString();
        Path dir = getDirUsuarioEmail(migracao);
        int status = run(dir, fullcmd, migracao.getEmail().getEmail(), migracao.getEmail().getUsuario().getUsuario() + "@unesp.br");
        return new Status(status, "");
    }

    public static class Status {
        private final int exitValue;
        private final String output;

        public Status(int exitValue, String output) {
            this.exitValue = exitValue;
            this.output = output;
        }

        public int getExitValue() {
            return exitValue;
        }

        public String getOutput() {
            return output;
        }

    }
}
