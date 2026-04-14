# Sistema de Telecomunicações Distribuído (Java Sockets)

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Sockets](https://img.shields.io/badge/TCP%2FUDP-Sockets-blue?style=for-the-badge)
![Concorrência](https://img.shields.io/badge/Threads-Multi--threaded-success?style=for-the-badge)

Este projeto é um sistema distribuído cliente-servidor para o gerenciamento de linhas telefônicas, serviços adicionais (Siga-me, Secretária) e registro de chamados de suporte. Ele foi desenvolvido com foco em comunicação de rede de baixo nível, descartando middlewares prontos (como RMI) em favor de uma arquitetura baseada em Sockets puros e um protocolo de serialização binária customizado.

## Principais Funcionalidades

- **Comunicação Híbrida (TCP/UDP):** - Comunicação **Unicast (TCP)** para troca de dados transacionais com garantia de entrega (Registro de reclamações, consultas de protocolo e autenticação).
  - Comunicação **Multicast (UDP)** rodando em Thread de segundo plano para o disparo de Notas Informativas Globais por administradores, alcançando todos os clientes simultaneamente.
- **Servidor Multi-threaded:** Capacidade de atender a múltiplos clientes simultaneamente através da alocação dinâmica de Threads (`TratadorClienteTCP`).
- **Serialização Customizada (POJOs puros):** Empacotamento e desempacotamento manual de objetos e mensagens em fluxos de bytes (`InputStream` / `OutputStream`), abolindo o uso da interface nativa `Serializable` do Java.
- **Protocolo de Aplicação (Length-Prefix):** Implementação de uma classe roteadora (`Pacote.java`) que estrutura as requisições em `[CÓDIGO AÇÃO] + [TAMANHO PAYLOAD] + [DADOS]`, blindando o servidor contra estouros de memória (Out-Of-Memory).

---

## Arquitetura do Sistema

O projeto é estruturado em três camadas lógicas principais:

1. **Camada de Domínio (POJOs):** - `Linha.java`, `Servico.java` (Classe base).
   - Demonstração de Herança e Polimorfismo através dos serviços `SigaMe` e `Secretaria`.
2. **Camada de Transporte e Serialização:**
   - `LinhaInputStream` e `LinhaOutputStream`: Classes customizadas que processam os atributos fisicamente (byte a byte).
   - `Pacote.java`: Define o contrato de empacotamento da comunicação.
3. **Camada de Aplicação:**
   - `ServidorTelecom.java`: Mantém o estado da aplicação em memória (via `ConcurrentHashMap`) e gerencia as conexões.
   - `ClienteTelecom.java`: Interface CLI interativa.

---

## Guia Interno: Funções Principais e Arquitetura

Este documento detalha o funcionamento interno dos principais métodos e classes do Sistema Telecom. A arquitetura foi desenhada para não depender de bibliotecas de alto nível de serialização, provando o domínio sobre fluxos de bytes, concorrência e redes.

---

## 1. O Protocolo de Aplicação (`Pacote.java`)
Esta classe atua como o "tradutor" entre o Cliente e o Servidor. Ela garante que os dados não cheguem corrompidos, implementando o padrão de rede **Length-Prefix** (onde o tamanho da mensagem é enviado antes da mensagem em si).

* **`empacotar(DataOutputStream out)`**
  * **O que faz:** Converte uma ação e seus dados em um fluxo binário puro e injeta no cabo de rede (TCP).
  * **Como funciona:** Ele escreve 3 coisas em sequência estrita: 
    1. O código da Ação (4 bytes).
    2. O tamanho da String de dados (4 bytes).
    3. Os bytes reais da String (`UTF-8`).
  * **Por que é importante:** Substitui a "mágica" de enviar objetos prontos, garantindo controle total sobre a banda de rede.

* **`desempacotar(DataInputStream in)`**
  * **O que faz:** Lê os bytes que chegam do Socket e reconstrói o objeto `Pacote`.
  * **A Proteção (Guard Rail):** Ele lê o tamanho do payload esperado. Se esse tamanho for absurdamente grande (ex: um ataque hacker ou erro de versão), ele bloqueia a alocação de memória e lança uma `IOException`, protegendo o servidor contra *Out-Of-Memory*. Em seguida, usa `readFully()` para garantir que nenhum byte fique para trás.

---

## 2. Serialização Customizada (`LinhaOut/InputStream.java`)
O núcleo do processamento de dados isolados. Estas classes lidam diretamente com os POJOs (`Linha`, `Servico`).

* **`enviarDados()` (em `LinhaOutputStream`)**
  * **O que faz:** Varre um array de objetos `Linha` e os converte byte a byte.
  * **Como funciona:** Cumprindo a regra de negócio exigida, o método grava o tamanho e depois o dado para exatamente 3 atributos: Número, Titular e Quantidade de Serviços.
  
* **`lerDados()` (em `LinhaInputStream`)**
  * **O que faz:** O processo de desserialização manual.
  * **Como funciona:** Instancia a classe original `Linha` na memória RAM puxando os dados crus do `InputStream` de origem. Pode ser usado para ler da rede, do console ou de um arquivo do disco rígido.

---

## 3. O Motor do Servidor (`ServidorTelecom.java`)
A classe principal que rege a comunicação e o estado global da aplicação.

* **`main(String[] args)` (O Loop de Aceitação)**
  * **O que faz:** Fica bloqueado no método `serverSocket.accept()` escutando a porta 8080.
  * **O Pulo do Gato:** Assim que um cliente conecta, ele *não* atende o cliente ali mesmo. Ele instancia uma `new Thread(new TratadorClienteTCP(clienteTCP)).start()`. Isso caracteriza o servidor como **Multi-threaded**, permitindo que 100 clientes registrem reclamações no exato mesmo milissegundo.

* **`TratadorClienteTCP.run()` (A Lógica de Roteamento)**
  * **O que faz:** É a Thread isolada de cada cliente.
  * **Como funciona:** Desempacota o request, usa uma estrutura `switch/case` baseada no ID da Ação (1 = Registrar, 2 = Consultar, etc.), acessa o "Banco de Dados em Memória" (`ConcurrentHashMap`, que é seguro para múltiplas threads) e devolve o protocolo empacotado.

* **`enviarMulticastUDP(String mensagem)`**
  * **O que faz:** Comunicação em massa de um para muitos.
  * **Como funciona:** Em vez de usar a conexão TCP segura, ele empacota a mensagem num `DatagramPacket` e joga para o endereço IP do grupo Multicast (`230.0.0.1` na porta `4446`). Qualquer máquina da rede que estiver escutando esse grupo receberá a nota administrativa instantaneamente.

---

## 4. A Interface do Cliente (`ClienteTelecom.java`)

* **`enviarRequisicaoTCP(int codigoAcao, String jsonEnvio)`**
  * **O que faz:** Encapsula o ciclo de vida completo de uma requisição **Unicast**.
  * **Como funciona:** Abre o Socket, instancia um `Pacote`, dispara os dados, fica aguardando a resposta, lê o novo `Pacote` recebido do servidor, imprime e fecha a conexão.

* **`ReceptorNotasUDP.run()` (A Thread Fantasma)**
  * **O que faz:** Um "rádio" que fica ligado nos bastidores do cliente.
  * **Como funciona:** É uma *Daemon Thread* que usa `MulticastSocket.joinGroup()`. Ela fica rodando em loop infinito separada do menu principal do usuário. Quando um pacote UDP chega do servidor, ela interrompe a tela e imprime o alerta para o usuário.

---

## 5. Bateria de Testes (`TestesStreams.java`)

* **`main(String[] args)`**
  * **O que faz:** Teste de Integração (End-to-End) do mecanismo de serialização.
  * **Conceito de Polimorfismo:** Demonstra o poder do encapsulamento em Java. Como o `LinhaOutputStream` espera um `OutputStream` genérico, o teste injeta três coisas diferentes nele: 
    1. Um `System.out` (escreve na tela).
    2. Um `FileOutputStream` (escreve no disco rígido).
    3. Um `Socket.getOutputStream()` (escreve pela placa de rede).
  * O código do `LinhaOutputStream` não muda uma vírgula para lidar com os três cenários, provando que a lógica está altamente desacoplada.
