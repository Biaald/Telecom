import java.io.*;
import java.net.*;

public class TestesStreams {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO BATERIA DE TESTES (LinhaOutputStream) ===\n");

        // 1. Preparando dados de teste (Aproveitando a sua estrutura)
        Linha[] linhasDeTeste = {
            new Linha("85999991111", "Ana Beatriz"),
            new Linha("11988882222", "Cicero Rodrigues")
        };
        int numObjetos = linhasDeTeste.length;

        // =========================================================
        // TESTE I: Saída Padrão (System.out)
        // =========================================================
        System.out.println(">>> TESTE I: Escrevendo na Saída Padrão (System.out) <<<");
        try {
            // Instanciamos passando o System.out nativo do Java
            LinhaOutputStream losConsole = new LinhaOutputStream(linhasDeTeste, numObjetos, System.out);
            losConsole.enviarDados();
            //System.out.println("\n(Nota: Os caracteres 'estranhos' acima são os bytes brutos impressos no console)\n");
        } catch (Exception e) {
            System.err.println("Erro no Teste I: " + e.getMessage());
        }

        // =========================================================
        // TESTE II: Arquivo (FileOutputStream)
        // =========================================================
        System.out.println(">>> TESTE II: Escrevendo em Arquivo Binário <<<");
        String nomeArquivo = "linhas_teste.bin";
        try (FileOutputStream fos = new FileOutputStream(nomeArquivo)) {
            // Instanciamos passando o manipulador de arquivos
            LinhaOutputStream losArquivo = new LinhaOutputStream(linhasDeTeste, numObjetos, fos);
            losArquivo.enviarDados();
            System.out.println("Sucesso! Arquivo '" + nomeArquivo + "' gerado com os bytes serializados no disco.\n");
        } catch (Exception e) {
            System.err.println("Erro no Teste II: " + e.getMessage());
        }

        // =========================================================
        // TESTE III: Servidor Remoto (TCP)
        // =========================================================
        System.out.println(">>> TESTE III: Escrevendo para Servidor Remoto (TCP Sockets) <<<");
        int portaTeste = 8080;

        // PASSO A: Subir um mini-servidor em uma Thread separada só para atuar como o "recebedor" do TCP
        Thread miniServidorTCP = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(portaTeste)) {
                System.out.println("[Servidor de Teste] Aguardando conexão TCP na porta " + portaTeste + "...");
                
                try (Socket cliente = server.accept();
                     InputStream is = cliente.getInputStream()) {

                    System.out.println("[Servidor de Teste] Conectado! Lendo bytes com LinhaInputStream...");
                    
                    // Validação: LinhaInputStream para prova que a rede funcionou
                    LinhaInputStream lis = new LinhaInputStream(is);
                    Linha[] linhasRecebidas = lis.lerDados();

                    System.out.println("[Servidor de Teste] Dados reconstruídos com sucesso via rede:");
                    for (Linha l : linhasRecebidas) {
                        System.out.println("-> Titular: " + l.getTitular() + " | Num: " + l.getNumeroTelefone());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Erro no Servidor] " + e.getMessage());
            }
        });
        miniServidorTCP.start();

        // Um pequeno atraso (meio segundo) para garantir que o servidor acima subiu antes de enviar
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // PASSO B: O "Cliente" abre o Socket e envia os dados pela rede usando o seu LinhaOutputStream
        try (Socket socketEnvio = new Socket("localhost", portaTeste);
             OutputStream outSocket = socketEnvio.getOutputStream()) {

            // Instanciamos passando o fluxo de saída do Socket TCP
            LinhaOutputStream losTCP = new LinhaOutputStream(linhasDeTeste, numObjetos, outSocket);
            losTCP.enviarDados();
            System.out.println("Sucesso (Cliente)! Bytes injetados no túnel TCP.\n");

        } catch (Exception e) {
            System.err.println("Erro no Teste III (Cliente): " + e.getMessage());
        }
    }
}