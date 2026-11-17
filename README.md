# Nokia Retro Launcher (T9)

Um **Launcher Android nativo** em Kotlin + Jetpack Compose com aparência fiel aos celulares clássicos da Nokia: visor LCD monocromático retrô na metade superior, teclado físico virtual T9 na metade inferior e grade real de aplicativos instalados ao abrir o `Menu`.

## Funcionalidades

- **Tela Inicial LCD Retrô**:
  - Fundo verde-oliva/mostarda clássico (`#A49F24`), com temas alternáveis (*Classic Mustard*, *Nokia 3310 Green*, *Retro Amber*).
  - Barra de status com antena/sinal, Wi-Fi, setas de dados e bateria com porcentagem real.
  - Data centralizada, divisória horizontal com aba angular exibindo o relógio em tempo real e softkeys (`Go to`, `Menu`, `Setting`).
- **Menu / App Drawer**:
  - Grade de 4 colunas com os ícones e nomes reais dos apps instalados no dispositivo.
  - Navegação por toque direto ou pelo **D-Pad** (setas direcionais + centro OK).
  - Filtro rápido via teclado **T9** ao digitar no Menu.
- **Teclado Físico Virtual T9**:
  - Softkeys esquerda/direita, botões de chamar (verde) e desligar/voltar (vermelho), D-Pad central e grade `0–9`, `*`, `#`.
  - Discagem direta pela tela inicial (pressione o botão verde para abrir o discador).
  - Segurar `0` liga/desliga a lanterna do aparelho.
- **Configurações**:
  - Alternância de tema do LCD, vibração háptica, tom retrô de tecla e atalho direto para definir como Launcher padrão do Android.

## Instalação (APK)

Baixe o arquivo `NokiaRetroLauncher-v1.0.0-release.apk` na aba **Releases** deste repositório e instale no Android (API 24+ / Android 7.0 ao 15).

## Compilação Local

Requisitos: JDK 17 e Android SDK (`compileSdk 35`, `minSdk 24`, `targetSdk 34`).

```bash
./gradlew assembleRelease
```
