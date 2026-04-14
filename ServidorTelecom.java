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
    
    // Bancos de dados em memória (Thread-safe para evitar conflitos)
    private static final Map<String, String> chamadosAbertos = new ConcurrentHashMap<>();
    private static final Map<String, String> bancoDeLinhas = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== SERVIDOR TELECOM MULTI-THREADED INICIADO ===");
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA_TCP)) {
            while (true) {
                Socket clienteTCP = serverSocket.accept();
                System.out.println("\n[TCP] Novo cliente conectado: " + clienteTCP.getInetAddress());
                
                // REQUISITO: Servidor Multi-threaded
                // Cria uma nova Thread para cada cliente conectado
                new Thread(new TratadorClienteTCP(clienteTCP)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // LÓGICA DE ATENDIMENTO DO CLIENTE (THREAD)
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
                
                // REQUISITO: Desempacotar a mensagem de requisição do cliente
                Pacote request = Pacote.desempacotar(dis);
                int acao = request.getCodigoAcao();
                String payload = request.getPayload(); // O JSON ou String de dados
                
                String respostaParaCliente = "";

                // Roteamento baseado no código da ação empacotada
                switch (acao) {
                    case 1: // REGISTRAR RECLAMAÇÃO
                        System.out.println("[AÇÃO 1] Cliente registrando reclamação. Dados: " + payload);
                        
                        String protocolo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        chamadosAbertos.put(protocolo, payload + " | Status: EM ANÁLISE");
                        
                        respostaParaCliente = "{\"status\": \"sucesso\", \"protocolo\": \"" + protocolo + "\"}";
                        System.out.println("       -> Protocolo gerado: " + protocolo);
                        break;

                    case 2: // CONSULTAR STATUS
                        System.out.println("[AÇÃO 2] Cliente consultando protocolo: " + payload);
                        
                        // Extrai o protocolo do JSON simples
                        String protoBusca = payload.split("\"protocolo\":\"")[1].split("\"")[0];
                        String status = chamadosAbertos.getOrDefault(protoBusca, "Protocolo não encontrado.");
                        
                        respostaParaCliente = "{\"status\": \"sucesso\", \"dados\": \"" + status + "\"}";
                        break;

                    case 3: // LOGIN ADMIN
                        System.out.println("[AÇÃO 3] Tentativa de Login Administrativo.");
                        if (payload.contains("\"senha\":\"" + SENHA_ADMIN + "\"")) {
                            respostaParaCliente = "{\"status\": \"sucesso\", \"mensagem\": \"Acesso Autorizado\"}";
                            System.out.println("       -> Login com sucesso.");
                        } else {
                            respostaParaCliente = "{\"status\": \"erro\", \"mensagem\": \"Senha Incorreta\"}";
                            System.out.println("       -> Falha no login.");
                        }
                        break;

                    case 4: // ENVIAR NOTA MULTICAST
                        System.out.println("[AÇÃO 4] Admin solicitou envio de nota informativa.");
                        String nota = payload.split("\"nota\":\"")[1].split("\"")[0];
                        
                        // Dispara o UDP
                        enviarMulticastUDP("NOTA OFICIAL: " + nota);
                        
                        respostaParaCliente = "{\"status\": \"sucesso\", \"mensagem\": \"Nota disparada para todos via UDP.\"}";
                        break;

                    default:
                        System.out.println("[ERRO] Código de ação desconhecido: " + acao);
                        respostaParaCliente = "{\"status\": \"erro\", \"mensagem\": \"Ação desconhecida\"}";
                }

                // REQUISITO: Empacotar a mensagem de reply e enviar para o cliente
                Pacote reply = new Pacote(acao, respostaParaCliente);
                reply.empacotar(dos);

            } catch (EOFException e) {
                // Fim da conexão
            } catch (IOException e) {
                System.err.println("Erro na comunicação com o cliente: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignora erro no fechamento
                }
            }
        }
    }

    // ==========================================
    // REQUISITO: COMUNICAÇÃO MULTICAST UDP
    // ==========================================
    private static void enviarMulticastUDP(String mensagem) {
        try (DatagramSocket socketUDP = new DatagramSocket()) {
            InetAddress grupo = InetAddress.getByName(GRUPO_MULTICAST);
            byte[] buffer = mensagem.getBytes("UTF-8");
            
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, grupo, PORTA_MULTICAST);
            socketUDP.send(pacote);
            
            System.out.println("       -> [UDP MULTICAST ENVIADO] " + mensagem);
        } catch (IOException e) {
            System.err.println("Erro ao enviar Multicast UDP: " + e.getMessage());
        }
    }
}
