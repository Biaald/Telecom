import java.io.*;

public class Pacote {
    private int codigoAcao; //  3 = Login, 4 = Registrar
    private String payload; // Os dados em si (JSON)

    public Pacote(int codigoAcao, String payload) {
        this.codigoAcao = codigoAcao;
        this.payload = payload;
    }

    public int getCodigoAcao() { return codigoAcao; }
    public String getPayload() { return payload; }

    // LADO QUE ENVIA: EMPACOTAMENTO (Serialização)
    public void empacotar(DataOutputStream out) throws IOException {
        // 1. Empacota o código da ação (4 bytes)
        out.writeInt(codigoAcao);
        
        // 2. Converte os dados em um array de bytes
        byte[] payloadBytes = payload.getBytes("UTF-8");
        
        // 3. Empacota o tamanho do array (4 bytes)
        out.writeInt(payloadBytes.length);
        
        // 4. Empacota os dados reais (N bytes)
        out.write(payloadBytes);
        out.flush();
    }

    // LADO QUE RECEBE: DESEMPACOTAMENTO (Desserialização)
    public static Pacote desempacotar(DataInputStream in) throws IOException {
        // 1. Desempacota o código da ação
        int acao = in.readInt();
        
        // 2. Desempacota o tamanho dos dados
        int tamanho = in.readInt();
        
        // PROTEÇÃO CONTRA OUT OF MEMORY 
        // Se o tamanho for negativo ou maior que 5 Megabytes (5 * 1024 * 1024 bytes), rejeita!
        if (tamanho < 0 || tamanho > 5242880) {
            throw new IOException("Tamanho de payload inválido ou corrompido: " + tamanho + " bytes.");
        }
        
        // 3. Lê exatamente a quantidade de bytes do payload
        byte[] buffer = new byte[tamanho];
        in.readFully(buffer); // Garante que lê tudo
        
        // 4. Reconstrói a string a partir dos bytes
        String dados = new String(buffer, "UTF-8");
        
        return new Pacote(acao, dados);
    }
}