import java.util.ArrayList;
import java.util.List;

// Agregação: uma linha telefônica possui serviços contratados.
public class Linha {
    private String numeroTelefone;
    private String titular;
    private List<Servico> servicosContratados;

    public Linha(String numeroTelefone, String titular) {
        this.numeroTelefone = numeroTelefone;
        this.titular = titular;
        this.servicosContratados = new ArrayList<>();
    }

    public void adicionarServico(Servico servico) {
        this.servicosContratados.add(servico);
        servico.setAtivo(true);
    }

    public List<Servico> getServicosContratados() { return servicosContratados; }
    public String getNumeroTelefone() { return numeroTelefone; }
    public String getTitular() { return titular; }
}