import java.io.*;
import java.net.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ServidorTelecom {
    // Configurações de Rede
    private static final int PORTA_TCP = 8080;
    private static final String GRUPO_MULTICAST = "230.0.0.1";
    private static final int PORTA_MULTICAST = 4446;
    private static final String SENHA_ADMIN = "123";
    private static final String ARQUIVO_BD = "chamados_db.json"; // <-- NOVO: Nome do arquivo
    
    // Bancos de dados em memória
    private static final Map<String, String> chamadosAbertos = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== SERVIDOR TELECOM INICIADO ===");
        
        // <-- NOVO: Carrega os dados antigos do arquivo JSON ao ligar o servidor
        carregarDadosDoDisco(); 
        
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA_TCP)) {
            while (true) {
                Socket clienteTCP = serverSocket.accept();
                System.out.println("\n[TCP] Novo cliente conectado: " + clienteTCP.getInetAddress());
                new Thread(new TratadorClienteTCP(clienteTCP)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // LÓGICA DE ATENDIMENTO DO CLIENTE
    // ==========================================
    static class TratadorClienteTCP implements Runnable {
        private final Socket socket;

        public TratadorClienteTCP(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                
                Pacote request = Pacote.desempacotar(dis);
                int acao = request.getCodigoAcao();
                String payload = request.getPayload();
                
                String respostaParaCliente = "";

                switch (acao) {
                    case 1: // REGISTRAR RECLAMAÇÃO
                        System.out.println("[AÇÃO 1] Cliente registrando reclamação. Dados: " + payload);
                        
                        String protocolo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        chamadosAbertos.put(protocolo, payload + " | Status: EM ANÁLISE");
                        
                        // <-- NOVO: Salva imediatamente no arquivo .json
                        salvarDadosNoDisco(); 
                        
                        respostaParaCliente = "{\"status\": \"sucesso\", \"protocolo\": \"" + protocolo + "\"}";
                        System.out.println("       -> Protocolo gerado e salvo no disco: " + protocolo);
                        break;

                    case 2: // CONSULTAR STATUS
                        System.out.println("[AÇÃO 2] Cliente consultando protocolo: " + payload);
                        String protoBusca = payload.split("\"protocolo\":\"")[1].split("\"")[0];
                        String status = chamadosAbertos.getOrDefault(protoBusca, "Protocolo não encontrado.");
                        respostaParaCliente = "{\"status\": \"sucesso\", \"dados\": \"" + status + "\"}";
                        break;

                    case 3: // LOGIN ADMIN
                        if (payload.contains("\"senha\":\"" + SENHA_ADMIN + "\"")) {
                            respostaParaCliente = "{\"status\": \"sucesso\", \"mensagem\": \"Acesso Autorizado\"}";
                        } else {
                            respostaParaCliente = "{\"status\": \"erro\", \"mensagem\": \"Senha Incorreta\"}";
                        }
                        break;

                    case 4: // ENVIAR NOTA MULTICAST
                        String nota = payload.split("\"nota\":\"")[1].split("\"")[0];
                        enviarMulticastUDP("NOTA OFICIAL: " + nota);
                        respostaParaCliente = "{\"status\": \"sucesso\", \"mensagem\": \"Nota disparada via UDP.\"}";
                        break;

                    default:
                        respostaParaCliente = "{\"status\": \"erro\", \"mensagem\": \"Ação desconhecida\"}";
                }

                Pacote reply = new Pacote(acao, respostaParaCliente);
                reply.empacotar(dos);

            } catch (EOFException e) {
            } catch (IOException e) {
                System.err.println("Erro na comunicação com o cliente: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }

    // ==========================================
    // MULTICAST UDP
    // ==========================================
    private static void enviarMulticastUDP(String mensagem) {
        try (DatagramSocket socketUDP = new DatagramSocket()) {
            InetAddress grupo = InetAddress.getByName(GRUPO_MULTICAST);
            byte[] buffer = mensagem.getBytes("UTF-8");
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, grupo, PORTA_MULTICAST);
            socketUDP.send(pacote);
        } catch (IOException e) {
            System.err.println("Erro ao enviar Multicast UDP: " + e.getMessage());
        }
    }

    // ==========================================
    // NOVAS FUNÇÕES: PERSISTÊNCIA EM ARQUIVO .JSON
    // ==========================================
    
    // 1. Salva os dados do mapa em um arquivo JSON de forma limpa (Sem usar bibliotecas externas)
    private static synchronized void salvarDadosNoDisco() {
        try (FileWriter fw = new FileWriter(ARQUIVO_BD)) {
            fw.write("{\n");
            int contador = 0;
            for (Map.Entry<String, String> entry : chamadosAbertos.entrySet()) {
                // Escapa as aspas internas para não quebrar a formatação do JSON
                String valorEscapado = entry.getValue().replace("\"", "\\\""); 
                fw.write("  \"" + entry.getKey() + "\": \"" + valorEscapado + "\"");
                
                contador++;
                if (contador < chamadosAbertos.size()) fw.write(",\n");
                else fw.write("\n");
            }
            fw.write("}");
        } catch (IOException e) {
            System.err.println("[ERRO] Não foi possível salvar o arquivo JSON: " + e.getMessage());
        }
    }

    // 2. Carrega os dados do arquivo JSON de volta para a RAM quando o servidor liga
    private static void carregarDadosDoDisco() {
        File arquivo = new File(ARQUIVO_BD);
        if (!arquivo.exists()) {
            System.out.println("[DB] Nenhum banco de dados anterior encontrado. Iniciando zerado.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                // Lógica simples para extrair chave e valor das linhas do JSON 
                if (linha.contains("\": \"")) {
                    String[] partes = linha.split("\": \"", 2);
                    String chave = partes[0].replace("\"", "").trim();
                    String valor = partes[1].replace("\",", "").replace("\"", "").replace("\\\"", "\"").trim();
                    chamadosAbertos.put(chave, valor);
                }
            }
            System.out.println("[DB] " + chamadosAbertos.size() + " chamados carregados do disco com sucesso!");
        } catch (IOException e) {
            System.err.println("[ERRO] Falha ao ler o arquivo JSON: " + e.getMessage());
        }
    }
}
