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

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class UnidadeConfigService {

    private static final Logger log = LoggerFactory.getLogger(UnidadeConfigService.class);

    private Map<String, Object> map;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    @Value("${configDir}")
    private String configDir;

    private FileWatcher fileWatcher;

    @PostConstruct
    public void init() {
        YamlMapFactoryBean factory = new YamlMapFactoryBean();
        Path unidades = Paths.get(configDir, "unidades.yml");
        UrlResource urlResource;
        try {
            urlResource = new UrlResource(unidades.toUri());
        } catch (MalformedURLException ex) {
            log.error("Erro cirando UrlResource", ex);
            throw new RuntimeException(ex);
        }
        factory.setResources(urlResource);
        map = factory.getObject();
        fileWatcher = new FileWatcher(unidades.toFile(), () -> {
            try {
                writeLock.lock();
                log.info("Reloading: unidades.yml");
                factory.setResources(urlResource);
                map = factory.getObject();
            } catch (Throwable ex) {
                log.error("Error reloading unidades.yml", ex);
            } finally {
                writeLock.unlock();
            }
        });
        fileWatcher.start();
    }

    @PreDestroy
    public void destroy() {
        fileWatcher.stopThread();
    }

    public boolean contem(String dominio) {
        try {
            readLock.lock();
            return map.containsKey(dominio);
        } finally {
            readLock.unlock();
        }
    }

    public String getServidor(String dominio) {
        try {
            readLock.lock();
            Map<String, String> config = (Map<String, String>) map.get(dominio);
            return config.get("servidor");
        } finally {
            readLock.unlock();
        }
    }

    public String getComandoSenha(String dominio) {
        try {
            readLock.lock();
            Map<String, Object> config = (Map<String, Object>) map.get(dominio);
            Map<String, String> commands = (Map<String, String>) config.get("comando");
            return commands.get("senha");
        } finally {
            readLock.unlock();
        }
    }

    public String getComandoValidar(String dominio) {
        try {
            readLock.lock();
            Map<String, Object> config = (Map<String, Object>) map.get(dominio);
            Map<String, String> commands = (Map<String, String>) config.get("comando");
            return commands.get("validar");
        } finally {
            readLock.unlock();
        }
    }

    public String getComandoContato(String dominio) {
        try {
            readLock.lock();
            Map<String, Object> config = (Map<String, Object>) map.get(dominio);
            Map<String, String> commands = (Map<String, String>) config.get("comando");
            return commands.get("contato");
        } finally {
            readLock.unlock();
        }
    }

    public String getComandoForward(String dominio) {
        try {
            readLock.lock();
            Map<String, Object> config = (Map<String, Object>) map.get(dominio);
            Map<String, String> commands = (Map<String, String>) config.get("comando");
            return commands.get("forward");
        } finally {
            readLock.unlock();
        }
    }

    public String getComandoMigrar(String dominio) {
        try {
            readLock.lock();
            Map<String, Object> config = (Map<String, Object>) map.get(dominio);
            Map<String, String> commands = (Map<String, String>) config.get("comando");
            return commands.get("migrar");
        } finally {
            readLock.unlock();
        }
    }

    public String getEmailNotificacao(String dominio) {
        try {
            readLock.lock();
            Map<String, String> config = (Map<String, String>) map.get(dominio);
            return config.get("email");
        } finally {
            readLock.unlock();
        }
    }

}
