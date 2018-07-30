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
import br.unesp.fc.migracaocontasemail.data.Migracao;
import br.unesp.fc.migracaocontasemail.data.MigracaoExec;
import br.unesp.fc.migracaocontasemail.data.Usuario;
import br.unesp.fc.migracaocontasemail.repository.EmailRepository;
import br.unesp.fc.migracaocontasemail.repository.MigracaoExecRepository;
import br.unesp.fc.migracaocontasemail.repository.MigracaoRepository;
import br.unesp.fc.migracaocontasemail.repository.UsuarioRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MigracaoRepository migracaoRepository;

    @Autowired
    private MigracaoExecRepository migracaoExecRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Transactional
    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Migracao salvar(Migracao migracao) {
        return migracaoRepository.save(migracao);
    }

    @Transactional
    public MigracaoExec salvar(MigracaoExec migracaoExec) {
        return migracaoExecRepository.save(migracaoExec);
    }

    @Transactional
    public List<Migracao> salvarMigracao(List<Migracao> lista) {
        return migracaoRepository.save(lista);
    }

    @Transactional(readOnly = true)
    List<Migracao> listarMigracaoPendentes() {
        return migracaoRepository.listarMigracaoPendente();
    }

    @Transactional(readOnly = true)
    public List<Email> buscarEmail(String usuario, String email) {
        return emailRepository.buscarPorUsuarioEmail(usuario, email);
    }

    @Transactional(readOnly = true)
    public Usuario buscarUsuario(String usuario) {
        return usuarioRepository.findByUsuario(usuario);
    }

    public List<Migracao> buscarMigracaoPorEmail(String email) {
        return migracaoRepository.listarMigracaoPorEmail(email);
    }

}
