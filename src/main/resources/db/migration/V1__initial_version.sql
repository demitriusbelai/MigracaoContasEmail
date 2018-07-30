CREATE TABLE usuario (
    id              SERIAL PRIMARY KEY,
    usuario         VARCHAR(60) NOT NULL,
    UNIQUE (usuario)
);

CREATE TABLE email (
    id              SERIAL PRIMARY KEY,
    id_usuario      INTEGER NOT NULL,
    email           VARCHAR(128) NOT NULL,
    dominio         VARCHAR(128) NOT NULL,
    erro            TEXT,
    migrar_emails   BOOLEAN NOT NULL,
    migrar_contatos BOOLEAN NOT NULL,
    FOREIGN KEY (id_usuario)
        REFERENCES usuario (id)
);

CREATE TABLE migracao (
    id              SERIAL PRIMARY KEY,
    id_email        INTEGER NOT NULL,
    horario         TIMESTAMP NOT NULL,
    completado      BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (id_email)
        REFERENCES email (id)
);

CREATE TABLE migracao_exec (
    id              SERIAL PRIMARY KEY,
    id_migracao     INTEGER NOT NULL,
    inicio          TIMESTAMP NOT NULL,
    termino         TIMESTAMP,
    status          INTEGER,
    FOREIGN KEY (id_migracao)
        REFERENCES migracao (id)
);
