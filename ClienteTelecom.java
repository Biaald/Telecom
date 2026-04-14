import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteTelecom {
    //private static final String HOST = "10.13.136.119";
    //private static final String HOST = "10.10.245.83";
    //private static final String HOST = "localhost";
    private static final String HOST = "10.163.102.119";
    private static final int PORTA_TCP = 8080;
    private static final String GRUPO_MULTICAST = "230.0.0.1";
    private static final int PORTA_MULTICAST = 4446;
    private static boolean isAdmin = false;

    public static void main(String[] args) {
        Scanner leitor = new Scanner(System.in);

        // Inicia a Thread para escutar notas via UDP Multicast
        Thread threadMulticast = new Thread(new ReceptorNotasUDP());
        threadMulticast.setDaemon(true);
        threadMulticast.start();

        System.out.println("=== CLIENTE TELECOM INICIADO ===");
        System.out.println("Escutando notas informativas em segundo plano...\n");

        int opcao = -1;
        while (opcao != 0) {
            System.out.println("1. Registrar Reclamação");
            System.out.println("2. Consultar Protocolo");
            System.out.println("3. Login Administrador");
            if (isAdmin) System.out.println("4. Enviar Nota Informativa");
            System.out.println("0. Sair");
            System.out.print("Escolha: ");
            
            try {
                opcao = Integer.parseInt(leitor.nextLine());

                switch (opcao) {
                    case 1 -> enviarRequisicaoTCP(1, "{\"acao\":\"registrar\", \"linha\":\"" + ler(leitor, "Linha") + "\", \"motivo\":\"" + ler(leitor, "Motivo") + "\"}");
                    case 2 -> enviarRequisicaoTCP(2, "{\"acao\":\"consultar\", \"protocolo\":\"" + ler(leitor, "Protocolo") + "\"}");
                    case 3 -> {
                        String resposta = enviarRequisicaoTCP(3, "{\"acao\":\"login\", \"senha\":\"" + ler(leitor, "Senha") + "\"}");
                        if (resposta.contains("sucesso")) isAdmin = true;
                    }
                    case 4 -> {
                        if (isAdmin) enviarRequisicaoTCP(4, "{\"acao\":\"enviarNota\", \"nota\":\"" + ler(leitor, "Nota") + "\"}");
                        else System.out.println("Opção inválida.");
                    }
                    case 0 -> System.out.println("Saindo...");
                    default -> System.out.println("Opção inválida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Por favor, digite um número válido.");
            }
        }
    }

    // ==========================================
    // REQUISITO: EMPACOTAMENTO E DESEMPACOTAMENTO
    // ==========================================
    private static String enviarRequisicaoTCP(int codigoAcao, String jsonEnvio) {
        try (Socket socket = new Socket(HOST, PORTA_TCP);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            
            // 1. CLIENTE EMPACOTA O REQUEST
            Pacote request = new Pacote(codigoAcao, jsonEnvio);
            request.empacotar(dos); // Envia os bytes para o servidor

            // 2. CLIENTE DESEMPACOTA O REPLY
            Pacote reply = Pacote.desempacotar(dis); // Lê os bytes do servidor
            
            System.out.println("\n[RESPOSTA DO SERVIDOR] " + reply.getPayload() + "\n");
            return reply.getPayload();

        } catch (IOException e) {
            System.err.println("Erro de conexão TCP: " + e.getMessage());
            return "";
        }
    }

    private static String ler(Scanner sc, String msg) {
        System.out.print(msg + ": ");
        return sc.nextLine();
    }

    // ==========================================
    // ESCUTA UDP MULTICAST
    // ==========================================
    static class ReceptorNotasUDP implements Runnable {
        public void run() {
            try (MulticastSocket socket = new MulticastSocket(PORTA_MULTICAST)) {
                InetAddress grupo = InetAddress.getByName(GRUPO_MULTICAST);
                
                InetAddress meuIP = InetAddress.getByName(HOST);
                NetworkInterface nif = NetworkInterface.getByInetAddress(meuIP);
                
                SocketAddress enderecoGrupo = new InetSocketAddress(grupo, PORTA_MULTICAST);
                
                // Entra no grupo escutando a placa de rede do Wi-Fi
                socket.joinGroup(enderecoGrupo, nif);
                
                byte[] buffer = new byte[2048];

                while (true) {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    socket.receive(pacote);
                    String mensagem = new String(pacote.getData(), 0, pacote.getLength(), "UTF-8");
                    System.out.println("\n\n [ALERTA MULTICAST UDP] " + mensagem + "\nEscolha: ");
                }
            } catch (IOException e) {
                System.err.println("Erro na Thread UDP: " + e.getMessage()); 
            }
        }
    }
}
