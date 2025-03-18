#!/bin/bash
set -e

# Создаем базу ticket_db
psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "CREATE DATABASE ticket_db;"
echo "Database ticket_db created."