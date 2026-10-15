# WhatsApp Messenger Clone — Arquitetura Nativa Poliglota

Este projeto é uma reconstrução de alta fidelidade do aplicativo **WhatsApp Messenger (Android)**, espelhando a arquitetura interna e utilizando **todas as linguagens de programação** originais identificadas durante a engenharia reversa do pacote oficial (v2.26.39.79):

---

## 1. Matriz de Linguagens Utilizadas

| Linguagem | Camada / Módulo | Arquivos no Projeto | Função Arquitetural |
| :--- | :--- | :--- | :--- |
| **Kotlin** | Camada de Aplicação & UI | `MainActivity.kt`, `ui/`, `viewmodel/`, `model/` | Interface moderna reativa com **Jetpack Compose**, gerenciamento de estado via **Coroutines / StateFlow** e reatividade |
| **Java** | Serviços & Broadcasts Android | `VoiceCallingService.java`, `DirectReplyReceiver.java`, `BootCompletedReceiver.java` | Gerenciamento de **Foreground Service** para chamadas VoIP de áudio/vídeo, escuta de boot do sistema e respostas diretas da barra de notificações |
| **C++ (C++17)** | Camada Nativa de Alta Performance | `app/src/main/cpp/native_bridge.cpp`, `CMakeLists.txt` | Ponte JNI (`libwhatsapp_native.so`), integração com subsistemas de hardware, aceleração ARM NEON e controle de VoIP |
| **C (ANSI C / C99)**| Algoritmos de Baixo Nível & Codecs | `crypto/curve25519.c`, `crypto/curve25519.h`, `codec/opus_helper.c`, `codec/opus_helper.h` | Implementação de tempo constante de **Curve25519** (troca de chaves do Protocolo Signal) e empacotamento de áudio RTP/Opus com cálculo de amplitude para waveform |
| **SQL** | Persistência Relacional & E2EE | `app/src/main/assets/schema/msgstore_schema.sql`, `axolotl_schema.sql` | Esquemas dos bancos de dados SQLite originais: `msgstore.db` (mensagens, conversas, mídias) e `axolotl.db` (chaves de identidade, sessões e pré-chaves pós-quânticas **Crystals-Kyber**) |
| **Protocol Buffers** | Protocolo de Rede & Serialização | `app/src/main/proto/whatsapp_message.proto` | Especificação formal (IDL) de stanzas binárias de mensageria, eventos de entrega e aperto de mão pós-quântico |

---

## 2. Estrutura de Diretórios do Projeto

```
D:\WHATZAPP_CLONE\WHATZAP\
├── build.gradle.kts                     (Configuração raiz do Gradle)
├── settings.gradle.kts                  (Resolução de módulos e repositórios)
├── gradle.properties                    (Flags de memória e JVM)
├── local.properties                     (Caminho local para o Android SDK)
├── app/
│   ├── build.gradle.kts                 (Configuração do módulo Android, Compose e NDK CMake)
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml      (Manifesto com permissões de chamada, câmera, mic e services)
│           ├── proto/
│           │   └── whatsapp_message.proto (Schema Protobuf oficial)
│           ├── assets/
│           │   └── schema/              (Schemas SQL originais msgstore e axolotl)
│           │       ├── msgstore_schema.sql
│           │       └── axolotl_schema.sql
│           ├── cpp/                     (Camada Nativa em C e C++ compilada via NDK)
│           │   ├── CMakeLists.txt
│           │   ├── native_bridge.cpp    (C++17 - Ponte JNI e Hardware Acceleration)
│           │   ├── crypto/
│           │   │   ├── curve25519.h     (C99 - Criptografia Signal Protocol RFC 7748)
│           │   │   └── curve25519.c
│           │   └── codec/
│           │       ├── opus_helper.h    (C99 - Processador de Áudio VoIP / Waveform)
│           │       └── opus_helper.c
│           ├── java/com/whatsapp/clone/ (Camada Java & Kotlin)
│           │   ├── MainActivity.kt      (Entrypoint com Jetpack Compose)
│           │   ├── nativebridge/
│           │   │   └── NativeBridge.kt  (Binding Kotlin JNI para libwhatsapp_native.so)
│           │   ├── services/
│           │   │   └── VoiceCallingService.java (Foreground Service para chamadas VoIP)
│           │   ├── receivers/
│           │   │   ├── DirectReplyReceiver.java (Inline notification reply)
│           │   │   └── BootCompletedReceiver.java (Reinicialização em segundo plano)
│           │   ├── model/
│           │   │   └── Models.kt        (Message, Chat, CallItem)
│           │   ├── viewmodel/
│           │   │   └── WhatsAppViewModel.kt (Orquestrador reativo e áudio nativo)
│           │   └── ui/
│           │       ├── theme/           (Paleta oficial WhatsApp: verde #00A884 e Dark Theme)
│           │       ├── components/      (ChatBubble, AudioWaveform, TopBar, BottomNav)
│           │       └── screens/         (HomeScreen, ChatDetailScreen, StatusScreen, CallsScreen, ActiveCallScreen)
│           └── res/
│               ├── raw/                 (Sons autênticos extraídos: kd3.ogg, ken.ogg, keq.ogg, etc.)
│               └── drawable/            (Ícone oficial app_icon.png)
```

---

## 3. Como Abrir e Executar

### Opção 1: No Android Studio
1. Abra o **Android Studio**.
2. Clique em **File -> Open** e selecione o diretório: `D:\WHATZAPP_CLONE\WHATZAP`.
3. O Android Studio sincronizará o Gradle, compilará o código nativo C++ com o NDK e indexará o projeto automaticamente.
4. Conecte um celular Android via USB ou inicie um emulador e clique em **Run (▶)**.

### Opção 2: Pela Linha de Comando (PowerShell)
```powershell
cd D:\WHATZAPP_CLONE\WHATZAP
$env:JAVA_HOME = "C:\Java\java-17"
& "C:\Users\emer7\.gradle\wrapper\dists\gradle-8.14-all\c2qonpi39x1mddn7hk5gh9iqj\gradle-8.14\bin\gradle.bat" assembleDebug
```
O APK final será gerado em: `app/build/outputs/apk/debug/app-debug.apk`.
