# Nokia Retro Launcher (T9)

Um **Launcher Android nativo** em Kotlin + Jetpack Compose com aparência fiel aos celulares clássicos da Nokia: visor LCD monocromático retrô na metade superior, teclado físico virtual T9 na metade inferior e grade real de aplicativos instalados ao abrir o `Menu`.

## Funcionalidades

- **Tela Inicial LCD Retrô**:
  - 8 temas clássicos de visor LCD (*Classic Mustard*, *Nokia 3310 Green*, *Retro Amber*, *Nokia 8210 Blue*, *Ice Cyan 8310*, *Paper White*, *Ruby Red 6610* e *Matrix Night*).
  - Barra de status com antena/sinal, ícone de busca rápida, lanterna, Wi-Fi, setas de dados e bateria com porcentagem real.
  - Suporte a **Papel de Parede**: presets retrô (*Pixel Grid*, *Retro Horizon*) ou foto própria da galeria do aparelho.
  - Data centralizada, faixa de atalhos rápidos favoritos, divisória horizontal com aba angular exibindo o relógio em tempo real e softkeys (`Go to`, `Menu`, `Setting`).
- **Sons Retrô Nokia (Sintetizador Piezo PCM)**:
  - Áudio sintetizado em tempo real (canal de mídia, independente do som de toque do sistema) com tons distintos para teclas T9 (`0–9`, `*`, `#`), navegação, confirmação, retorno, chamada, lanterna, acorde de inicialização e o toque clássico **Nokia Tune (Gran Vals)**.
- **Menu / App Drawer & Busca**:
  - Grade de 4 colunas com os ícones e nomes reais dos apps instalados no dispositivo.
  - Barra superior com ícone de busca interativa por texto e filtro rápido pelo teclado **T9**.
  - Navegação por toque direto ou pelo **D-Pad** (4 ícones direcionais ao redor do botão central **OK**).
- **Teclado Físico Virtual Retroiluminado**:
  - Retroiluminação LED configurável nas teclas (`Sync LCD`, `Green LED`, `Blue LED`, `Amber LED`, `White LED` ou `OFF`).
  - D-Pad principal exibindo os 4 ícones dos atalhos nas direções (Câmera, Mensagens, Atalhos Rápidos, Configurações) ao redor do botão central **OK**.
  - Discagem direta pela tela inicial (pressione o botão verde para abrir o discador).
  - Segurar `1–9` na tela inicial abre os atalhos rápidos favoritos; segurar `0` liga/desliga a lanterna.
- **Jogo da Cobrinha (Snake II)**:
  - Clássico *Snake II* jogável dentro do próprio visor LCD usando o D-Pad ou teclas `2 / 4 / 6 / 8` e `5`, com recorde salvo localmente e efeitos sonoros retrô.
- **Multi-idioma e Atalhos Rápidos**:
  - Troca de idioma em tempo real nas configurações (*Auto*, *Português*, *English*, *Español*).
  - Gerenciador dedicado de atalhos rápidos (também acessível segurando o ícone no Menu ou pela tecla `#`).

## Instalação (APK)

Baixe o arquivo `NokiaRetroLauncher-v1.1.0-release.apk` na aba **Releases** deste repositório e instale no Android (API 24+ / Android 7.0 ao 15).

## Compilação Local

Requisitos: JDK 17 e Android SDK (`compileSdk 35`, `minSdk 24`, `targetSdk 34`).

```bash
./gradlew assembleRelease
```
