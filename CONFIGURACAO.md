# 🔧 Guia de Configuração Final - WalkKing

## ✅ Já Configurado
- ✅ Projeto Firebase: `walking-df5d0` 
- ✅ Código atualizado para usar seu projeto
- ✅ Build compilando sem erros

## 🚀 Próximos Passos Obrigatórios

### 1. **Configurar App Android no Firebase Console**

**Acesse:** https://console.firebase.google.com/project/walking-df5d0

1. **Adicionar App Android:**
   - Clique no ícone **Android** ou "Adicionar app"
   - **Nome do pacote Android**: `com.msystem.walking`
   - **Nome do app (opcional)**: `WalkKing`
   - **SHA-1**: Deixe em branco por enquanto
   - Clique em **"Registrar app"**

2. **Baixar google-services.json:**
   - O Firebase gerará um arquivo específico do seu projeto
   - **IMPORTANTE**: Baixe e substitua o arquivo atual em `app/google-services.json`
   - Este arquivo contém as chaves corretas do seu projeto

### 2. **Ativar Authentication**

1. **No Firebase Console**, vá em **Authentication**
2. Clique em **"Vamos começar"**
3. Vá na aba **"Método de login"**
4. **Ativar Email/Senha:**
   - Clique em "Email/senha"
   - Ative a primeira opção
   - Salvar

5. **Ativar Google Sign-In:**
   - Clique em "Google"
   - Ativar
   - Escolha um email de suporte
   - **COPIAR O WEB CLIENT ID** que aparecerá
   - Salvar

6. **Atualizar código com Web Client ID:**
   - Substitua no arquivo `AuthRepository.java` na linha 44:
   ```java
   .requestIdToken("COLE_O_WEB_CLIENT_ID_AQUI")
   ```

### 3. **Criar Firestore Database**

1. **No Firebase Console**, vá em **Firestore Database**
2. Clique em **"Criar banco de dados"**
3. **Regras de segurança:**
   - Escolha **"Iniciar no modo de teste"**
   - Clique em "Próximo"
4. **Local do banco:**
   - Escolha **"southamerica-east1 (São Paulo)"**
   - Clique em "Concluído"

### 4. **Configurar Google Maps API**

1. **Acesse:** https://console.cloud.google.com/
2. **Selecione o projeto:** `walking-df5d0`
3. **Ativar APIs:**
   - Vá em "APIs e serviços" → "Biblioteca"
   - Procure por **"Maps SDK for Android"**
   - Clique e ative
4. **Criar Chave de API:**
   - Vá em "APIs e serviços" → "Credenciais"
   - Clique em "+ CRIAR CREDENCIAIS" → "Chave de API"
   - **Copie a chave gerada**
5. **Adicionar no app:**
   - Substitua no `AndroidManifest.xml` na linha 22:
   ```xml
   android:value="COLE_SUA_GOOGLE_MAPS_API_KEY_AQUI"
   ```

## 🧪 **Teste da Configuração**

### Após completar os passos acima:

1. **Sync do projeto no Android Studio**
2. **Build do projeto:**
   ```
   .\gradlew build
   ```
3. **Instalar no dispositivo:**
   ```
   .\gradlew installDebug
   ```

## 📋 **Checklist de Configuração**

- [ ] App Android adicionado no Firebase Console
- [ ] google-services.json baixado e substituído
- [ ] Authentication ativado (Email/Senha + Google)
- [ ] Web Client ID copiado e colado no código
- [ ] Firestore Database criado
- [ ] Maps SDK for Android ativado no Google Cloud
- [ ] API Key do Google Maps criada e adicionada
- [ ] Build do projeto executado com sucesso
- [ ] App testado no dispositivo

## 🐛 **Solução de Problemas Comuns**

### Erro de autenticação Google:
- Verifique se o Web Client ID está correto
- Certifique-se de que o Google Sign-In está ativado no Firebase

### Mapa não carrega:
- Verifique se a API Key do Google Maps está correta
- Confirme se o Maps SDK for Android está ativado

### Erro de permissões:
- Aceite todas as permissões solicitadas no dispositivo
- Verifique se as permissões estão no AndroidManifest.xml

## 🎯 **Resultado Final**

Após seguir todos os passos, seu app WalkKing estará:
- ✅ Conectado ao Firebase real
- ✅ Com autenticação funcionando
- ✅ Com banco de dados ativo
- ✅ Com mapas integrados
- ✅ Pronto para rastrear caminhadas e conquistar territórios!

## 📞 **Suporte**

Se encontrar algum problema, verifique:
1. Se todos os serviços estão ativados no Firebase Console
2. Se as chaves de API estão corretas no código
3. Se o google-services.json foi baixado do projeto correto
