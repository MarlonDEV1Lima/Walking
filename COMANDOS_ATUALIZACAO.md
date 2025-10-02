# Configuração de Depuração USB - Guia Completo

## **Passo 1: Ativar Opções do Desenvolvedor**

### Para a maioria dos Android:
1. Abra **Configurações** no seu celular
2. Vá em **Sobre o telefone** (ou **Sobre o dispositivo**)
3. Procure por **"Número da versão"** ou **"Número da compilação"**
4. Toque **7 vezes seguidas** no número da versão
5. Aparecerá a mensagem: *"Você agora é um desenvolvedor!"*

### Localização específica por marca:
- **Samsung**: Configurações → Sobre o telefone → Informações do software → Número da versão
- **Xiaomi**: Configurações → Sobre o telefone → Versão MIUI
- **Huawei**: Configurações → Sobre o telefone → Número da versão
- **LG**: Configurações → Sobre o telefone → Informações do software → Número da versão
- **Motorola**: Configurações → Sobre o telefone → Número da versão

## **Passo 2: Ativar Depuração USB**

1. Volte ao menu principal de **Configurações**
2. Procure por **"Opções do desenvolvedor"** ou **"Opções de desenvolvedor"**
   - Se não aparecer, role até o final das configurações
3. Entre em **Opções do desenvolvedor**
4. Ative o botão principal (no topo) se estiver desativado
5. Procure por **"Depuração USB"** e **ATIVE**
6. Confirme clicando em **"OK"** quando aparecer o aviso

## **Passo 3: Conectar ao Computador**

1. Conecte o celular ao computador via **cabo USB**
2. No celular, aparecerá uma notificação sobre **"Uso do USB"**
3. Toque na notificação e selecione **"Transferência de arquivos"** ou **"MTP"**
4. Pode aparecer um popup perguntando se confia no computador → marque **"Sempre permitir"** e toque **"OK"**

## **Passo 4: Verificar Conexão**

Abra o terminal/prompt no seu projeto e execute:

```bash
# Verificar se o dispositivo foi detectado
adb devices
```

**Resultado esperado:**
```
List of devices attached
ABC123456789    device
```

Se aparecer "unauthorized", vá no celular e aceite a conexão.

## **Passo 5: Instalar App Diretamente**

Agora você pode usar qualquer um destes métodos:

### **Método 1: Script Automático (Mais Fácil)**
```bash
.\install_app.bat
```

### **Método 2: Android Studio (Recomendado)**
1. Abra o projeto no Android Studio
2. Clique no botão **"Run"** 
3. Selecione seu dispositivo na lista
4. **Pronto!** App instala automaticamente

### **Método 3: Comando Manual**
```bash
# Compilar e instalar em um comando
.\gradlew assembleDebug && adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

# Comandos rápidos para atualização

## **Comandos Essenciais**

### 1. Verificar dispositivos conectados
```bash
adb devices
```

### 2. Compilar + Instalar automaticamente
```bash
.\gradlew assembleDebug && adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 3. Executar script automático
```bash
.\install_app.bat
```

### 4. Apenas compilar (se preferir instalar manualmente)
```bash
.\gradlew assembleDebug
```

### 5. Instalar APK já compilado
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 6. Desinstalar versão anterior (se necessário)
```bash
adb uninstall com.msystem.walking
```

### 7. Ver logs em tempo real (para debug)
```bash
adb logcat | findstr "Walking"
```

## **Comandos de Troubleshooting**

### Se o dispositivo não aparecer:
```bash
# Reiniciar servidor ADB
adb kill-server
adb start-server
adb devices
```

### Se der erro de "device unauthorized":
1. Vá no celular e aceite a conexão
2. Execute novamente:
```bash
adb devices
```

### Para forçar reinstalação:
```bash
adb uninstall com.msystem.walking
.\gradlew assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```

## **Comandos Rápidos por Situação**

### **Desenvolvimento diário:**
```bash
# Fazer mudanças no código → executar:
.\gradlew assembleDebug && adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### **Primeira instalação:**
```bash
.\install_app.bat
```

### **Debug com logs:**
```bash
adb logcat | findstr "MainActivity\|Territory\|Firebase"
```

### **Limpar e reinstalar:**
```bash
adb uninstall com.msystem.walking
.\gradlew clean assembleDebug
adb install app\build\outputs\apk\debug\app-debug.apk
```

---

## **Checklist de Configuração**

- [ ] Opções do desenvolvedor ativadas (toque 7x na versão)
- [ ] Depuração USB ativada
- [ ] Celular conectado via USB
- [ ] "Transferência de arquivos" selecionado
- [ ] Computador autorizado no celular
- [ ] `adb devices` mostra o dispositivo
- [ ] Android Studio detecta o dispositivo

## **Resultado Final**

Após essa configuração, você pode:
- Instalar app diretamente do Android Studio (clique Run)
- Usar comandos rápidos para atualizar
- Debug em tempo real com logs
- **Nunca mais precisar gerar APK manualmente!**
