#!/bin/bash
# Script di installazione MySQL Manager
set -e

APP_USER="mysqlmgr"
INSTALL_DIR="/opt/mysql-manager"
LOG_DIR="/var/log/mysql-manager"
CONFIG_DIR="/etc/mysql-manager"
SERVICE_NAME="mysql-manager"
JAR_FILE="target/mysql-manager-1.0.0.jar"

echo "=== MySQL Manager Install ==="

# Crea utente di sistema
if ! id "$APP_USER" &>/dev/null; then
    useradd --system --no-create-home --shell /usr/sbin/nologin "$APP_USER"
    echo "Utente '$APP_USER' creato"
fi

# Crea directory
mkdir -p "$INSTALL_DIR" "$LOG_DIR" "$CONFIG_DIR"
chown "$APP_USER:$APP_USER" "$INSTALL_DIR" "$LOG_DIR"
chmod 750 "$INSTALL_DIR" "$LOG_DIR"

# Copia JAR
cp "$JAR_FILE" "$INSTALL_DIR/mysql-manager.jar"
chown "$APP_USER:$APP_USER" "$INSTALL_DIR/mysql-manager.jar"
chmod 640 "$INSTALL_DIR/mysql-manager.jar"

# Crea file env se non esiste
if [ ! -f "$CONFIG_DIR/env" ]; then
    cp deploy/env.example "$CONFIG_DIR/env"
    chown "$APP_USER:$APP_USER" "$CONFIG_DIR/env"
    chmod 600 "$CONFIG_DIR/env"
    echo ""
    echo "IMPORTANTE: configura le password in $CONFIG_DIR/env prima di avviare il servizio!"
fi

# Installa servizio systemd
cp deploy/mysql-manager.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable "$SERVICE_NAME"

echo ""
echo "=== Installazione completata ==="
echo "1. Configura $CONFIG_DIR/env con le password reali"
echo "2. Avvia il servizio: systemctl start $SERVICE_NAME"
echo "3. Controlla lo stato: systemctl status $SERVICE_NAME"
echo "4. Log: journalctl -u $SERVICE_NAME -f"
