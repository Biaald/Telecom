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
