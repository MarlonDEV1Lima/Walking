# WalkKing / StepEmpire 🚶‍♂️👑

## Descrição
WalkKing é um aplicativo Android inovador que transforma caminhadas e corridas em uma experiência gamificada de conquista de territórios. Cada trajeto percorrido pelo usuário se torna um território conquistado no mapa, criando um sistema competitivo e social que incentiva a prática de exercícios.

## 🎯 Funcionalidades Principais

### ✅ Autenticação de Usuário
- **Firebase Authentication** com login por email/senha
- **Google Sign-In** para acesso rápido
- Criação automática de perfil de usuário

### ✅ Rastreamento GPS em Tempo Real
- Captura precisa de localização durante atividades
- Cálculo automático de distância percorrida
- Serviço em background para rastreamento contínuo
- Interface em tempo real com cronômetro e estatísticas

### ✅ Sistema de Territórios
- **Google Maps integrado** para visualização
- Criação automática de territórios baseados na rota
- Polígonos coloridos diferenciando proprietários
- Algoritmo inteligente de conquista territorial

### ✅ Gamificação Completa
- **Sistema de pontos**: 10 pontos por quilômetro percorrido
- **Conquista de territórios** automática durante atividades
- **Medalhas e conquistas** baseadas em desempenho
- Cores personalizadas por usuário

### ✅ Ranking Global (Leaderboard)
- Classificação por pontos totais
- Estatísticas detalhadas (distância, territórios)
- Interface moderna com destaces para top 3
- Atualização em tempo real

### ✅ Histórico Completo
- Registro detalhado de todas as atividades
- Visualização de data, duração, distância e pontos
- Interface intuitiva com cards organizados
- Filtros por tipo de atividade

## 🛠️ Tecnologias Utilizadas

### Backend e Dados
- **Firebase Authentication** - Autenticação segura
- **Firebase Firestore** - Banco de dados NoSQL em tempo real
- **Google Play Services** - Integração com serviços Google

### Mapas e Localização
- **Google Maps API** - Visualização de mapas e territórios
- **Google Location Services** - GPS de alta precisão
- **FusedLocationProviderClient** - Otimização de bateria

### Interface e Experiência
- **Material Design 3** - Interface moderna e consistente
- **View Binding** - Binding seguro de views
- **RecyclerView** - Listas performáticas
- **LiveData** - Observação reativa de dados

### Arquitetura
- **Repository Pattern** - Separação de responsabilidades
- **MVVM-like** - Arquitetura escalável
- **Services** - Processamento em background
- **Singleton Pattern** - Gerenciamento de instâncias

## 📱 Estrutura do Aplicativo

```
com.msystem.walking/
├── auth/              # Autenticação e login
├── model/             # Classes de dados
├── repository/        # Acesso a dados
├── service/           # Serviços em background
├── tracking/          # Rastreamento de atividades
├── leaderboard/       # Sistema de ranking
├── history/           # Histórico de atividades
└── utils/             # Utilitários e helpers
```

## 🚀 Como Configurar

### 1. Pré-requisitos
- Android Studio Arctic Fox ou superior
- SDK Android 21+ (Android 5.0+)
- Conta no Google Cloud Platform
- Conta no Firebase

### 2. Configuração do Firebase
1. Crie um projeto no [Firebase Console](https://console.firebase.google.com/)
2. Ative **Authentication** com Email/Password e Google
3. Ative **Cloud Firestore** em modo de teste
4. Baixe o arquivo `google-services.json`
5. Substitua o arquivo existente em `app/google-services.json`

### 3. Configuração do Google Maps
1. Acesse o [Google Cloud Console](https://console.cloud.google.com/)
2. Ative a **Maps SDK for Android**
3. Gere uma API Key
4. Substitua `YOUR_GOOGLE_MAPS_API_KEY` no `AndroidManifest.xml`

### 4. Configuração do Google Sign-In
1. No Firebase Console, configure o Google Sign-In
2. Baixe o Web Client ID
3. Substitua `YOUR_WEB_CLIENT_ID` no `AuthRepository.java`

## 📊 Sistema de Pontuação

### Pontos por Atividade
- **10 pontos** por quilômetro percorrido
- **Bônus de território** por área conquistada
- **Bônus de consistência** para atividades regulares

### Conquista de Territórios
- Território criado a cada 100 metros percorridos
- Raio de 50 metros por território
- Algoritmo inteligente evita sobreposições

## 🎮 Como Usar

### 1. Primeiro Acesso
1. Faça login com email/senha ou Google
2. Permita acesso à localização
3. Explore o mapa inicial

### 2. Iniciar Atividade
1. Toque no botão "▶️" na tela principal
2. Escolha entre caminhada ou corrida
3. Inicie o rastreamento
4. Acompanhe estatísticas em tempo real

### 3. Finalizar Atividade
1. Toque em "Finalizar" quando terminar
2. Veja territórios conquistados automaticamente
3. Confira pontos ganhos
4. Atividade salva no histórico

### 4. Competir
1. Acesse o Ranking para ver sua posição
2. Visualize territórios no mapa principal
3. Confira histórico detalhado

## 🏗️ Arquitetura Técnica

### Padrões Implementados
- **Repository Pattern** para abstração de dados
- **Observer Pattern** com LiveData
- **Singleton** para repositórios globais
- **Service Locator** para injeção de dependência

### Fluxo de Dados
```
UI Layer → Repository → Firebase/Google Services
    ↓           ↑
LiveData ← ViewModel Pattern
```

### Gerenciamento de Estado
- **LiveData** para observação reativa
- **SharedPreferences** para configurações locais
- **Firebase Firestore** para sincronização em nuvem

## 🔒 Segurança e Privacidade

### Dados Protegidos
- Autenticação Firebase com criptografia
- Regras de segurança no Firestore
- Validação de permissões em tempo real

### Privacidade
- Localização processada localmente
- Dados anonimizados no ranking
- Controle total sobre dados pessoais

## 🚀 Funcionalidades Futuras

### Versão 2.0 (Roadmap)
- [ ] **Chat social** entre usuários
- [ ] **Desafios semanais** e eventos
- [ ] **Integração com wearables**
- [ ] **Modo offline** com sincronização
- [ ] **Análise avançada** de performance
- [ ] **Compartilhamento social** de conquistas

### Melhorias Técnicas
- [ ] **Cache inteligente** para mapas
- [ ] **Otimização de bateria** avançada
- [ ] **Sincronização incremental**
- [ ] **Modo escuro** completo

## 📈 Métricas de Sucesso

### Engagement
- Tempo médio de uso por sessão
- Frequência de atividades por usuário
- Retenção de usuários (D1, D7, D30)

### Gamificação
- Territórios conquistados por usuário
- Progressão no ranking
- Completion rate de desafios

## 👥 Contribuição

### Como Contribuir
1. Fork o repositório
2. Crie uma branch para sua feature
3. Implemente seguindo os padrões
4. Teste completamente
5. Abra um Pull Request

### Padrões de Código
- **Material Design Guidelines**
- **Android Architecture Components**
- **Clean Architecture principles**
- **Documentação completa**

## 📄 Licença

Este projeto está sob licença MIT. Veja o arquivo `LICENSE` para mais detalhes.

## 🤝 Suporte

Para dúvidas ou suporte:
- 📧 Email: suporte@walkking.app
- 🐛 Issues: GitHub Issues
- 💬 Discord: WalkKing Community

---

**WalkKing** - Transforme cada passo em uma conquista! 👑🚶‍♂️
