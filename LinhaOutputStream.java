import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LinhaOutputStream extends OutputStream {
    private OutputStream outTarget;
    private Linha[] linhas;
    private int numObjetos;

    // Construtor
    public LinhaOutputStream(Linha[] linhas, int numObjetos, OutputStream outTarget) {
        this.linhas = linhas;
        this.numObjetos = numObjetos;
        this.outTarget = outTarget;
    }

    // Método obrigatório por herdar de OutputStream
    @Override
    public void write(int b) throws IOException {
        outTarget.write(b);
    }

    // Método responsável por serializar e enviar os dados
    public void enviarDados() throws IOException {
        DataOutputStream dos = new DataOutputStream(outTarget);
        
        // Envia a quantidade de objetos primeiro
        dos.writeInt(numObjetos);
        
        for (int i = 0; i < numObjetos; i++) {
            Linha linha = linhas[i];
            
            // Atributo 1: Número do Telefone
            byte[] numeroBytes = linha.getNumeroTelefone().getBytes();
            dos.writeInt(numeroBytes.length); // Envia tamanho em bytes
            dos.write(numeroBytes);           // Envia os dados
            
            // Atributo 2: Titular
            byte[] titularBytes = linha.getTitular().getBytes();
            dos.writeInt(titularBytes.length); // Envia tamanho em bytes
            dos.write(titularBytes);           // Envia os dados
            
            // Atributo 3: Quantidade de Serviços Contratados (inteiro simulado)
            int qtdServicos = linha.getServicosContratados().size();
            dos.writeInt(4); // Um 'int' padrão em Java ocupa 4 bytes
            dos.writeInt(qtdServicos);
        }
        dos.flush();
    }
}