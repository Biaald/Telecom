import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LinhaInputStream extends InputStream {
    private InputStream inOrigin;

    // Construtor (Recebe o InputStream de origem)
    public LinhaInputStream(InputStream inOrigin) {
        this.inOrigin = inOrigin;
    }

    // Método obrigatório por herdar de InputStream
    @Override
    public int read() throws IOException {
        return inOrigin.read();
    }

    // Método responsável por ler os bytes da rede/arquivo e reconstruir os objetos
    public Linha[] lerDados() throws IOException {
        DataInputStream dis = new DataInputStream(inOrigin);
        
        // 1. Lê a quantidade total de objetos no pacote
        int numObjetos = dis.readInt();
        Linha[] linhasLidas = new Linha[numObjetos];
        
        for (int i = 0; i < numObjetos; i++) {
            // Reconstruindo Atributo 1: Número do Telefone
            int tamanhoNumero = dis.readInt();
            byte[] numeroBytes = new byte[tamanhoNumero];
            dis.readFully(numeroBytes);
            String numeroTelefone = new String(numeroBytes);
            
            // Reconstruindo Atributo 2: Titular
            int tamanhoTitular = dis.readInt();
            byte[] titularBytes = new byte[tamanhoTitular];
            dis.readFully(titularBytes);
            String titular = new String(titularBytes);
            
            // Reconstruindo Atributo 3: Quantidade de Serviços
            int tamanhoQtdServicos = dis.readInt(); // Lê os 4 bytes indicativos (regra do trabalho)
            int qtdServicos = dis.readInt();        // Lê o valor inteiro real
            
            // Recria a instância do objeto POJO
            Linha linhaReconstruida = new Linha(numeroTelefone, titular);
            
            // Se precisar recriar os serviços simulados:
            // for (int j = 0; j < qtdServicos; j++) { 
            //     linhaReconstruida.adicionarServico(new SigaMe(0.0)); 
            // }

            linhasLidas[i] = linhaReconstruida;
        }
        
        return linhasLidas;
    }
}