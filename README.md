# MySQL Manager

Applicativo web autonomo per la gestione di server MySQL, costruito con Spring Boot 3.x.
Nessun Nginx/Apache richiesto — gira come JAR self-contained con Tomcat embedded.
Autenticazione a due fattori (MFA/TOTP) **obbligatoria** per tutti gli utenti.

## Funzionalità

- **Gestione database**: elenca, crea ed elimina database MySQL
- **Navigazione tabelle**: struttura colonne, `SHOW CREATE TABLE`
- **Query SQL**: editor con risultati a tabella (max 1000 righe)
- **Gestione utenti MySQL**: crea/elimina utenti, assegna/revoca privilegi, cambia password
- **Gestione utenti applicazione**: ruoli ADMIN e OPERATOR, abilitazione/disabilitazione
- **Backup e ripristino**: dump/restore tramite `mysqldump`
- **Audit log**: ogni operazione viene registrata con utente, IP, SQL e tempo di esecuzione
- **MFA obbligatorio**: TOTP compatibile con Google Authenticator, Aegis, Authy

## Stack

| Componente | Tecnologia |
|---|---|
| Backend | Java 21 + Spring Boot 3.2 |
| Sicurezza | Spring Security 6 + MFA/TOTP |
| Persistenza | Spring Data JPA + MySQL |
| Frontend | Thymeleaf + CSS puro |
| Deploy | JAR self-contained (systemd opzionale) |

---

## Requisiti

- **Java 21** (OpenJDK o Oracle JDK)
- **Maven 3.6+**
- **Git**
- **MySQL 8.x** o **MariaDB 10.6+**
- `mysqldump` e `mysql` disponibili nel PATH (per backup/ripristino)
- App TOTP sul telefono: Google Authenticator, Aegis, Authy

---

## Installazione dipendenze (Debian 13 / Ubuntu)

### Java 21

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
```

### Maven

```bash
sudo apt install -y maven
mvn -version
```

> Su Ubuntu 22.04 o versioni precedenti il pacchetto `maven` potrebbe installare una versione troppo vecchia.
> In quel caso scarica Maven manualmente:
>
> ```bash
> MVN_VERSION=3.9.9
> wget https://downloads.apache.org/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz
> sudo tar -xzf apache-maven-${MVN_VERSION}-bin.tar.gz -C /opt
> sudo ln -s /opt/apache-maven-${MVN_VERSION}/bin/mvn /usr/local/bin/mvn
> mvn -version
> ```

### Git

```bash
sudo apt install -y git
git --version
```

### MySQL / MariaDB

```bash
# MySQL 8
sudo apt install -y mysql-server

# oppure MariaDB
sudo apt install -y mariadb-server

sudo systemctl enable --now mysql   # oppure mariadb
```

### Tutto in un colpo solo

```bash
sudo apt update && sudo apt install -y openjdk-21-jdk maven git mysql-server
```

---

## Build

```bash
git clone https://github.com/tuo-utente/mysql-manager.git
cd mysql-manager
mvn clean package -DskipTests
```

Il JAR self-contained viene generato in:

```
target/mysql-manager-1.0.5.jar
```

---

## Configurazione database

Prima di avviare, crea lo schema e l'utente dedicati all'applicazione:

```sql
CREATE DATABASE mysql_manager_app
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'mysql_manager_user'@'localhost' IDENTIFIED BY 'SceglieUnaPasswordSicura!';

GRANT ALL PRIVILEGES ON mysql_manager_app.* TO 'mysql_manager_user'@'localhost';

FLUSH PRIVILEGES;
```

> `mysql_manager_app` è lo schema interno usato per utenti app, sessioni e audit log.
> È separato dai database che gestirai tramite l'interfaccia.

---

## Avvio manuale (senza systemd)

Questa modalità è adatta allo sviluppo o a server senza systemd.

### 1. Configura `application.properties`

Modifica `src/main/resources/application.properties` (prima della build) oppure passa le proprietà da riga di comando:

```properties
# Connessione schema applicazione
spring.datasource.username=mysql_manager_user
spring.datasource.password=SceglieUnaPasswordSicura!

# MySQL da gestire (può essere lo stesso server)
mysql.manager.host=127.0.0.1
mysql.manager.port=3306
mysql.manager.user=root
mysql.manager.password=TuaPasswordRoot
```

### 2. Avvia il JAR

```bash
java -jar target/mysql-manager-1.0.5.jar
```

Oppure, sovrascrivendo le proprietà da riga di comando senza toccare il file:

```bash
java -jar target/mysql-manager-1.0.5.jar \
  --spring.datasource.password=SceglieUnaPasswordSicura! \
  --mysql.manager.password=TuaPasswordRoot
```

Per sviluppo con ricarica automatica dei template:

```bash
mvn spring-boot:run
```

L'applicazione sarà disponibile su [http://localhost:8080](http://localhost:8080).

### 3. Avvio in background (nohup)

Per tenerla attiva dopo la chiusura del terminale, senza systemd:

```bash
nohup java -jar target/mysql-manager-1.0.5.jar \
  --spring.datasource.password=SceglieUnaPasswordSicura! \
  --mysql.manager.password=TuaPasswordRoot \
  > /var/log/mysql-manager.log 2>&1 &

echo $! > /var/run/mysql-manager.pid
```

Arresto:

```bash
kill $(cat /var/run/mysql-manager.pid)
```

---

## Deploy in produzione con systemd

### 1. Build

```bash
mvn clean package -DskipTests
```

### 2. Installa con lo script automatico

```bash
sudo bash deploy/install.sh
```

Lo script esegue in automatico:
- Creazione utente di sistema `mysqlmgr` (senza shell)
- Creazione directory `/opt/mysql-manager` e `/var/log/mysql-manager`
- Copia del JAR in `/opt/mysql-manager/mysql-manager.jar`
- Creazione configurazione in `/etc/mysql-manager/env`
- Installazione e abilitazione del servizio systemd

### 3. Configura le variabili d'ambiente

```bash
sudo nano /etc/mysql-manager/env
```

```env
SPRING_DATASOURCE_PASSWORD=SceglieUnaPasswordSicura!
MYSQL_MANAGER_PASSWORD=TuaPasswordRoot
```

```bash
sudo chmod 600 /etc/mysql-manager/env
sudo chown mysqlmgr:mysqlmgr /etc/mysql-manager/env
```

### 4. Avvia il servizio

```bash
sudo systemctl start mysql-manager
sudo systemctl status mysql-manager
```

### 5. Comandi utili

```bash
# Log in tempo reale
journalctl -u mysql-manager -f

# Riavvio
sudo systemctl restart mysql-manager

# Abilitazione automatica all'avvio
sudo systemctl enable mysql-manager
```

### 6. Aggiornamento

```bash
mvn clean package -DskipTests
sudo cp target/mysql-manager-1.0.5.jar /opt/mysql-manager/mysql-manager.jar
sudo systemctl restart mysql-manager
```

---

## Primo accesso

Al primo avvio viene creato automaticamente un utente admin:

| Campo | Valore |
|---|---|
| Username | `admin` |
| Password | `Admin@1234!` |

> **Cambia subito la password** dal pannello Admin dopo il primo accesso.

Il login segue tre passaggi:
1. Inserisci username e password
2. Scansiona il QR code con l'app Authenticator **(solo al primo accesso)**
3. Inserisci il codice OTP a 6 cifre

---

## Disinstallazione

### Disinstallazione — avvio manuale (nohup / terminale)

```bash
# Ferma il processo se avviato con nohup
kill $(cat /var/run/mysql-manager.pid) 2>/dev/null || true

# Rimuovi il file PID
rm -f /var/run/mysql-manager.pid

# Rimuovi il log (opzionale)
rm -f /var/log/mysql-manager.log

# Rimuovi il JAR e la directory del progetto
rm -rf /percorso/mysql-manager
```

Se stai usando un file `.env` o `application.properties` personalizzato fuori dalla directory del progetto, rimuovilo manualmente.

Per rimuovere anche lo schema MySQL dell'applicazione:

```sql
DROP DATABASE IF EXISTS mysql_manager_app;
DROP USER IF EXISTS 'mysql_manager_user'@'localhost';
FLUSH PRIVILEGES;
```

---

### Disinstallazione — servizio systemd

```bash
# Ferma e disabilita il servizio
sudo systemctl stop mysql-manager
sudo systemctl disable mysql-manager

# Rimuovi il file di servizio
sudo rm -f /etc/systemd/system/mysql-manager.service
sudo systemctl daemon-reload
sudo systemctl reset-failed

# Rimuovi file applicazione
sudo rm -rf /opt/mysql-manager
sudo rm -rf /var/log/mysql-manager
sudo rm -rf /etc/mysql-manager

# Rimuovi l'utente di sistema
sudo userdel mysqlmgr

# Rimuovi lo schema MySQL dell'applicazione (opzionale)
# Esegui come root MySQL:
# DROP DATABASE IF EXISTS mysql_manager_app;
# DROP USER IF EXISTS 'mysql_manager_user'@'localhost';
# FLUSH PRIVILEGES;
```

---

## Struttura del progetto

```
mysql-manager/
├── src/
│   └── main/
│       ├── java/com/mysqlmanager/
│       │   ├── config/          # SecurityConfig, TotpConfig, DataSourceConfig
│       │   ├── controller/      # Auth, Dashboard, Database, Query, Users, Admin
│       │   ├── domain/          # Entità JPA (AppUser, AuditLog)
│       │   ├── dto/             # QueryResultDto
│       │   ├── exception/       # GlobalExceptionHandler
│       │   ├── repository/      # JPA Repositories
│       │   ├── security/        # UserDetails, MfaAuthenticationFilter
│       │   └── service/         # TotpService, DatabaseManagerService, ...
│       └── resources/
│           ├── application.properties
│           ├── application-prod.properties
│           ├── schema.sql
│           ├── static/css/      # Stili CSS
│           └── templates/       # Template Thymeleaf
├── deploy/
│   ├── mysql-manager.service    # Unit systemd
│   ├── env.example              # Template variabili ambiente
│   └── install.sh               # Script di installazione
└── pom.xml
```

---

## Ruoli utente

| Ruolo | Permessi |
|---|---|
| `ADMIN` | Tutto: CREATE/DROP database e tabelle, gestione utenti MySQL, gestione utenti app, backup, audit log |
| `OPERATOR` | Solo SELECT/SHOW/DESCRIBE — nessuna operazione di scrittura o DDL |

---

## Flusso autenticazione MFA

```
POST /login  (username + password)
    └─► ROLE_PRE_AUTH assegnato

GET/POST /mfa/setup   ← solo al primo accesso (mostra QR code)
    └─► TOTP secret salvato, MFA attivato

GET/POST /mfa/verify  ← ad ogni login successivo
    └─► Codice OTP verificato
    └─► ROLE_ADMIN o ROLE_OPERATOR assegnato
    └─► Accesso garantito
```

---

## Sicurezza

- Password applicazione hashate con **BCrypt** (strength 12)
- Sessioni con timeout a **30 minuti**, massimo **1 sessione** per utente
- Session fixation protection attiva
- CSRF protection attiva su tutti i form POST
- Identificatori SQL validati con whitelist regex `[a-zA-Z0-9_$]{1,64}`
- Password MySQL gestite tramite `PreparedStatement` (mai interpolate)
- Operazioni DDL/DML registrate nell'audit log con utente, IP e timestamp
- Systemd: utente dedicato `mysqlmgr` senza shell, `NoNewPrivileges`, `PrivateTmp`, `ProtectSystem=strict`

---

## Variabili d'ambiente (produzione systemd)

| Variabile | Descrizione |
|---|---|
| `SPRING_DATASOURCE_PASSWORD` | Password utente schema applicazione (`mysql_manager_user`) |
| `MYSQL_MANAGER_PASSWORD` | Password utente MySQL da gestire |

Impostare in `/etc/mysql-manager/env` (chmod 600, chown mysqlmgr).

---

## Gerarchia di configurazione

Quando il JAR viene spostato in una directory isolata dal progetto, Spring Boot carica la configurazione nel seguente ordine di priorità (il più alto vince):

### 1. Variabili d'ambiente (priorità massima)

Spring Boot mappa automaticamente le variabili d'ambiente alle properties, convertendo il nome in maiuscolo e sostituendo `.` con `_`.

Esempi:

| Variabile d'ambiente | Property equivalente |
|---|---|
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` |
| `MYSQL_MANAGER_PASSWORD` | `mysql.manager.password` |

In produzione con systemd, vengono caricate da `/etc/mysql-manager/env` tramite `EnvironmentFile=` nel file di servizio.

Per un avvio manuale:

```bash
SPRING_DATASOURCE_PASSWORD=xxx \
MYSQL_MANAGER_PASSWORD=yyy \
java -jar /qualsiasi/path/mysql-manager.jar --spring.profiles.active=prod
```

### 2. Argomenti da riga di comando (priorità alta)

```bash
java -jar mysql-manager.jar \
  --spring.datasource.password=xxx \
  --mysql.manager.password=yyy
```

### 3. File di configurazione esterni (priorità media)

Spring Boot cerca automaticamente questi file nella **directory di lavoro** al momento dell'avvio (quella in cui si esegue il comando `java -jar`), anche se il JAR si trova altrove:

- `./application.properties`
- `./config/application.properties`

In alternativa è possibile indicare un percorso esplicito:

```bash
java -jar mysql-manager.jar \
  --spring.config.additional-location=/etc/mysql-manager/app.properties
```

### 4. Configurazione embedded nel JAR (priorità bassa)

I file `application.properties` e `application-prod.properties` sono inclusi nel JAR al momento della build e fungono da valori di default. Non richiedono nessun file esterno, ma **non sono adatti alla produzione** perché contengono le credenziali in chiaro.

Il profilo `prod` viene attivato con `--spring.profiles.active=prod` e sovrascrive alcune properties (cache Thymeleaf, log level) senza ridefinire le password.

---

### Riepilogo: cosa serve a runtime

Il JAR è completamente autonomo. Per avviarlo in qualsiasi directory è sufficiente fornire le credenziali tramite variabili d'ambiente o argomenti:

```bash
# Minimo indispensabile
java -jar /opt/mysql-manager/mysql-manager.jar \
  --spring.datasource.username=mysql_manager_user \
  --spring.datasource.password=SceglieUnaPasswordSicura! \
  --mysql.manager.password=TuaPasswordRoot \
  --spring.profiles.active=prod
```

Non è necessario che il file `application.properties` del progetto sorgente sia presente nella stessa directory del JAR.

---

## Aggiornamento delle dipendenze

### Verificare dipendenze obsolete

```bash
mvn versions:display-dependency-updates
```

Mostra l'elenco di tutte le dipendenze con la versione attuale e l'ultima disponibile, senza modificare nulla:

```
[INFO] The following dependencies in Dependencies have newer versions:
[INFO]   org.springframework.boot:spring-boot-starter-parent ... 3.2.4 -> 3.4.1
[INFO]   com.mysql:mysql-connector-j ...................... 8.3.0 -> 9.2.0
```

### Aggiornare automaticamente il pom.xml

```bash
mvn versions:use-latest-releases
```

Riscrive il `pom.xml` con le versioni più recenti di tutte le dipendenze.

Per aggiornare solo le patch (consigliato in produzione):

```bash
mvn versions:use-latest-versions -DallowMajorUpdates=false -DallowMinorUpdates=false
```

### Verificare anche i plugin

```bash
mvn versions:display-plugin-updates
```

### Procedura consigliata

```bash
# 1. Aggiorna il pom.xml
mvn versions:use-latest-releases

# 2. Verifica che tutto compili
mvn clean package -DskipTests

# 3. Esegui i test
mvn test
```

---

## Licenza

MIT
