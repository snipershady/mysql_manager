# MyApp — Database Manager

## Stack
- Java 21 + Spring Boot 3.x
- Spring Security (MFA/TOTP)
- Spring Data JPA + MySQL
- Thymeleaf
- Deploy: JAR self-contained + systemd

## Obiettivo
Applicativo web autonomo per gestire MySQL,
senza Nginx/Apache, con MFA obbligatorio,


## Stato attuale
- [x] Setup progetto Maven (pom.xml con tutte le dipendenze)
- [x] Configurazione Spring Security + MFA (TOTP via dev.samstevens.totp)
- [x] Controller gestione DB (database, tabelle, query, utenti MySQL)
- [x] Frontend Thymeleaf (layout, auth, dashboard, db, users, admin)
- [x] Audit log completo
- [x] Deploy systemd (deploy/mysql-manager.service + install.sh)
- [ ] Build JAR e deploy su server

## Build
```
/usr/bin/mvn clean package -DskipTests
```

## Configurazione
- `src/main/resources/application.properties` — imposta le password DB
- `deploy/env.example` → copiare in `/etc/mysql-manager/env`

## Credenziali default (CAMBIARE SUBITO)
- Utente app: admin / Admin@1234!
