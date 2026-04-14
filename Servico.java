// 1. Superclasse de Serviços 
public abstract class Servico {
    private String nome;
    private double valorMensal;
    private boolean ativo;

    public Servico(String nome, double valorMensal) {
        this.nome = nome;
        this.valorMensal = valorMensal;
        this.ativo = false; // Começa inativo por padrão
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public double getValorMensal() { return valorMensal; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}

// 2. Subclasses de Serviços
class SigaMe extends Servico {
    private String numeroDestino; // Número para onde a chamada será desviada

    public SigaMe(double valorMensal) {
        super("Siga-me", valorMensal);
    }
    
    public String getNumeroDestino() { return numeroDestino; }
    public void setNumeroDestino(String numeroDestino) { this.numeroDestino = numeroDestino; }
}

class Secretaria extends Servico {
    private int limiteMensagens; // Limite de mensagens na caixa postal

    public Secretaria(double valorMensal, int limiteMensagens) {
        super("Secretária Eletrônica", valorMensal);
        this.limiteMensagens = limiteMensagens;
    }
    
    public int getLimiteMensagens() { return limiteMensagens; }
}