# Sistema de Gerenciamento de Telecomunicações (Sistemas Distribuídos)

Este projeto implementa uma solução de comunicação entre processos (IPC) utilizando **Java RMI**, **Sockets TCP** e a criação de um **Protocolo de Serialização Binário Customizado**. 

O objetivo é gerenciar serviços de uma operadora de telefonia, permitindo o registro de reclamações e a ativação de serviços através de diferentes métodos de transporte de dados.

---

## Arquitetura do Projeto

O projeto está dividido em quatro camadas principais:

### 1. Domínio (POJOs)
* **`Servico.java`**: Superclasse abstrata que define a estrutura base dos serviços (Siga-me, Secretária).
* **`Linha.java`**: Objeto principal que representa o cliente (Titular, Número, Lista de Serviços).

### 2. Protocolo de Serialização (Streams Customizados)
* **`LinhaOutputStream.java`**: Implementa a lógica de escrita manual de objetos em bytes.
* **`LinhaInputStream.java`**: Implementa a lógica de leitura e reconstrução de objetos a partir de bytes.
> **Nota:** Não utiliza `ObjectOutputStream` padrão do Java, garantindo um protocolo binário otimizado.

### 3. Comunicação Remota (Middleware)
* **`Reclamacao.java`**: Interface remota (RMI).
* **`ReclamacaoServiceImpl.java`**: Lógica de negócio do servidor.
* **`ServidorRMI.java`**: Publica o serviço no `rmiregistry`.
* **`ClienteRMI.java`**: Interface interativa para o usuário final.

### 4. Validação e Testes
* **`TestesStreams.java`**: Teste automatizado que valida os Streams em:
    * **Arquivos** (`FileOutputStream`)
    * **Rede** (`Sockets TCP`)
    * **Console** (`System.out` / `System.in`)

---

## Como Executar

Siga a ordem abaixo para garantir que o ambiente esteja configurado corretamente.

### 1. Compilação
Abra o terminal na pasta do projeto e compile todos os arquivos:
```bash
javac *.java
```
---
### 2. Infraestrutura de Dados (Streams Customizados)

Nesta camada, implementamos a lógica de baixo nível para transformar objetos em sequências de bytes (binário), atendendo aos requisitos de serialização manual do trabalho.

### `LinhaOutputStream.java` (Escrita)
Subclasse de `OutputStream` responsável por percorrer um array de objetos `Linha` e escrever seus atributos no destino seguindo o protocolo:
1. **Controle:** Escreve a quantidade total de objetos (`writeInt`).
2. **Atributos:** Para cada `Linha`, extrai o `Titular` e `Número`.
3. **Protocolo:** Envia o tamanho da String (`writeInt`) seguido pelos bytes reais da String (`write`).
4. **Simulação:** Envia um valor inteiro de 4 bytes representando a quantidade de serviços.

### `LinhaInputStream.java` (Leitura)
Subclasse de `InputStream` que realiza o processo inverso:
1. Lê o cabeçalho para saber quantos objetos processar.
2. Reconstrói as Strings lendo exatamente a quantidade de bytes indicada pelos inteiros de tamanho.
3. Instancia novos objetos `Linha` com os dados recuperados do fluxo binário.

---

### 3. Comunicação Remota (RMI)

O sistema utiliza o middleware **Java RMI** para permitir que o cliente execute métodos em um objeto que reside em outra Máquina Virtual (JVM).

### `Reclamacao.java` (Interface)
Define o contrato de serviço. Todos os métodos aqui declarados podem ser chamados remotamente pelo cliente.
* `registrarReclamacao(String, String)`: Registro simples de texto.
* `consultarStatusReclamacao(String)`: Busca no banco de dados do servidor.
* `ativarServicoOtimizado(byte[])`: Método que recebe dados serializados pelos **Streams Customizados**.

### `ReclamacaoServiceImpl.java` (Implementação)
Contém a lógica de negócio e o "banco de dados" em memória (`HashMap`). É aqui que o servidor processa as requisições e gera os protocolos de atendimento.

### `ServidorRMI.java` e `ClienteRMI.java`
O **Servidor** inicializa o `LocateRegistry` e publica o objeto remoto. O **Cliente** utiliza um `Scanner` para interagir com o usuário e o `Naming.lookup` para encontrar o servidor na rede.

---

### 4. Validação e Testes

O arquivo **`TestesStreams.java`** foi criado para garantir que os streams funcionem de forma polimórfica em diferentes meios de transporte:

1. **Teste com Arquivo:** Grava a lista de linhas em `linhas_dados.bin` e lê de volta para validar a integridade.
2. **Teste com Sockets TCP:** Cria um `ServerSocket` em uma Thread e um `Socket` cliente em outra. Os objetos são transmitidos via rede local (localhost) através dos streams customizados.
3. **Teste com System IO:** Demonstra a escrita de dados binários diretamente no console (`System.out`).

---

### Fluxo de Execução Resumido

O diagrama abaixo ilustra como os componentes se conectam durante a execução da **Opção 3** (Ativação via Stream):



1. **Cliente:** Lê dados -> `LinhaOutputStream` -> `byte[]`.
2. **Rede:** Envia `byte[]` via RMI.
3. **Servidor:** Recebe `byte[]` -> `LinhaInputStream` -> Objeto `Linha`.

---

### Requisitos Atendidos (Trabalho 1)

| Item | Descrição | Status |
| :--- | :--- | :--- |
| **1** | Definição de serviço remoto e classes POJO. |OK|
| **2.a** | Subclasse `OutputStream` com envio de array e 3 atributos. |OK|
| **2.b** | Testes em System.out, Arquivo e Servidor TCP. |OK|
| **3.a** | Subclasse `InputStream` para reconstrução de dados. |OK|
| **3.b/c/d** | Testes em System.in, FileInputStream e Servidor TCP. |OK|

---
