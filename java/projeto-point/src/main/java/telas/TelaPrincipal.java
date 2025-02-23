/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package telas;

import banco.Alerta;
import banco.ConexaoPipefySlack;
import banco.Database;
import com.github.britooo.looca.api.core.Looca;
import com.github.britooo.looca.api.group.discos.DiscoGrupo;
import com.github.britooo.looca.api.group.sistema.Sistema;
import com.github.britooo.looca.api.group.memoria.Memoria;
import com.github.britooo.looca.api.group.processador.Processador;
import com.github.britooo.looca.api.group.processos.ProcessoGrupo;
import com.github.britooo.looca.api.group.servicos.ServicoGrupo;
import com.github.britooo.looca.api.group.temperatura.Temperatura;
import java.util.Timer;
import java.util.TimerTask;

import banco.Utilitarios;
import banco.Funcionario;
import banco.Localizacao;
import banco.Maquina;
import banco.Registros;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URL;

public class TelaPrincipal extends javax.swing.JFrame {

    private Looca looca = new Looca();
    private Sistema sistema = looca.getSistema();
    private DiscoGrupo grupoDeDiscos = looca.getGrupoDeDiscos();
    private ServicoGrupo grupoDeServicos = looca.getGrupoDeServicos();
    private ProcessoGrupo grupoDeProcessos = looca.getGrupoDeProcessos();
    private Temperatura temperatura = looca.getTemperatura();
    private Memoria memoria = looca.getMemoria();
    private Processador processador = looca.getProcessador();
    private Maquina maquina;
    private Database banco;
    
    private Funcionario func;

    public TelaPrincipal(Database banco, Funcionario func) {        
        initComponents();
        this.setResizable(false);
        this.looca = new Looca();
        this.banco = banco;
        this.setUpOs();

        this.func = func;
        
        this.maquina = new Maquina(banco);
        Registros registro = new Registros(banco);
        ConexaoPipefySlack conexao = new ConexaoPipefySlack(func);
        
        if (maquina.isMaquinaCadastrada(func)) {
            System.out.println("Máquina já cadastrada");
        }
        else if (maquina.isCadastrarMaquina(func)){
            System.out.println("Máquina Cadastrada");
        }
        else{
            System.out.println("Não foi possível encontrar máquina/cadastar máquina");
        }
        
        cpu.setText(Utilitarios.limitarDuasCasasDecimais(looca.getProcessador().getUso()).toString() + "%");
        usoRam.setText(Utilitarios.limitarDuasCasasDecimais(Utilitarios.converterBytesParaGiga(looca.getMemoria().getEmUso())).toString() + " GB ("
            + (Utilitarios.limitarDuasCasasDecimais((double) looca.getMemoria().getEmUso() / looca.getMemoria().getTotal() * 100).toString()) + "%)");
        totalRam.setText(Utilitarios.limitarDuasCasasDecimais(Utilitarios.converterBytesParaGiga(looca.getMemoria().getTotal())).toString() + " GB");
        Double espacoUtilizado = registro.getVolumeTotal() - registro.getVolumeDisponivel();
        grupoDeDisco.setText(Utilitarios.limitarDuasCasasDecimais(espacoUtilizado).toString() + " GB / " + registro.getVolumeTotal().toString() + " GB (" + Utilitarios.limitarDuasCasasDecimais(registro.getPorcentagemVolume()) + "%)");
        
        try{
            ObjectMapper mapper = new ObjectMapper();
            Localizacao local = mapper.readValue(new URL("https://ipinfo.io/json"), Localizacao.class);
            local.inserirLocalizacao(banco, "E", maquina.getId(), local);
        }
        catch (Exception e){
            System.out.println("Não foi possível inserir a entrada");
            System.out.println(e);
        }

        int delay = 5000;
        int interval = 1000;
        Timer timer = new Timer();
        Alerta alerta = new Alerta(banco, maquina);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Double porcentagemRam = (double) looca.getMemoria().getEmUso() / looca.getMemoria().getTotal() * 100;
                Double porcentagemCpu = looca.getProcessador().getUso();
                Double porcentagemDisco = Utilitarios.limitarDuasCasasDecimais(registro.getPorcentagemVolume());
                
                usoRam.setText(Utilitarios.limitarDuasCasasDecimais(Utilitarios.converterBytesParaGiga(looca.getMemoria().getEmUso())).toString() + " GB ("
                    + (Utilitarios.limitarDuasCasasDecimais(porcentagemRam).toString()) + "%)");
                cpu.setText(Utilitarios.limitarDuasCasasDecimais(porcentagemCpu).toString() + "%");
                Double espacoUtilizado = registro.getVolumeTotal() - registro.getVolumeDisponivel();
                grupoDeDisco.setText(Utilitarios.limitarDuasCasasDecimais(espacoUtilizado).toString() + " GB / " + registro.getVolumeTotal().toString() +
                    " GB (" + porcentagemDisco + "%)");
                
                registro.inserirRegistros(maquina);
                
                if(porcentagemCpu >= 80){
                    if(alerta.verificarUltimoAlerta("CPU").equals("FECHADO")){
                        alerta.criarAlerta("CPU", porcentagemCpu);
                        conexao.enviarAlerta(String.format("CPU está com %.1f%%!", porcentagemCpu), func);
                        System.out.println("Alerta inserido!");
                    }
                }
                else{
                    if(alerta.verificarUltimoAlerta("CPU").equals("ABERTO")){
                        alerta.fecharAlerta("CPU");
                    }
                }
                
                if(porcentagemRam >= 85){
                    if(alerta.verificarUltimoAlerta("RAM").equals("FECHADO")){
                        alerta.criarAlerta("RAM", porcentagemRam);
                        conexao.enviarAlerta(String.format("RAM está com %.1f%%!", porcentagemRam), func);
                        System.out.println("Alerta inserido!");
                    }
                }
                else{
                    if(alerta.verificarUltimoAlerta("RAM").equals("ABERTO")){
                        alerta.fecharAlerta("RAM");
                    }
                }
                
                if(porcentagemDisco >= 90){
                    if(alerta.verificarUltimoAlerta("DISCO").equals("FECHADO")){
                        alerta.criarAlerta("DISCO", porcentagemDisco);
                        conexao.enviarAlerta(String.format("Disco está com %.1f%%!", porcentagemDisco), func);
                        System.out.println("Alerta inserido!");
                    }
                }
                else{
                    if(alerta.verificarUltimoAlerta("DISCO").equals("ABERTO")){
                        alerta.fecharAlerta("DISCO");
                    }
                }
            }
        }, delay, interval);

    }

    private void setUpOs() {
        Sistema sistema = looca.getSistema();
        lblSistemaOperacionalValue.setText(String.format("▶ %s", sistema.getSistemaOperacional()));
        lblFabricanteValue.setText(String.format("▶ %s", sistema.getFabricante()));
        lblArquiteturaValue.setText(String.format("▶ %d bits", sistema.getArquitetura()));

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lblSistemaOperacionalValue = new javax.swing.JLabel();
        lblFabricanteValue = new javax.swing.JLabel();
        lblArquiteturaValue = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        totalRam = new javax.swing.JLabel();
        cpu = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        usoRam = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        grupoDeDisco = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        btnFechar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setBackground(new java.awt.Color(51, 255, 51));

        jLabel1.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        jLabel1.setText("Fabricante:");

        jLabel2.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        jLabel2.setText("Sistema Operacional:");
        jLabel2.setMaximumSize(new java.awt.Dimension(119, 25));

        jLabel3.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        jLabel3.setText("Arquitetura:");

        lblSistemaOperacionalValue.setFont(new java.awt.Font("sansserif", 0, 15)); // NOI18N
        lblSistemaOperacionalValue.setText("--");

        lblFabricanteValue.setFont(new java.awt.Font("sansserif", 0, 15)); // NOI18N
        lblFabricanteValue.setText("--");

        lblArquiteturaValue.setFont(new java.awt.Font("sansserif", 0, 15)); // NOI18N
        lblArquiteturaValue.setText("--");

        totalRam.setFont(new java.awt.Font("sansserif", 0, 15)); // NOI18N
        totalRam.setText("--");

        cpu.setFont(new java.awt.Font("sansserif", 0, 15)); // NOI18N
        cpu.setText("--");

        jLabel7.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        jLabel7.setText("CPU:");

        jLabel8.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        jLabel8.setText("Total da memória RAM:");

        jLabel9.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        jLabel9.setText("Uso de RAM:");

        usoRam.setFont(new java.awt.Font("sansserif", 0, 15)); // NOI18N
        usoRam.setText("--");

        jLabel10.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        jLabel10.setText("Discos:");

        grupoDeDisco.setFont(new java.awt.Font("sansserif", 0, 15)); // NOI18N
        grupoDeDisco.setText("--");

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/PointPequeno.png"))); // NOI18N

        btnFechar.setBackground(new java.awt.Color(204, 0, 0));
        btnFechar.setForeground(new java.awt.Color(255, 255, 255));
        btnFechar.setText("Fechar");
        btnFechar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFecharActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel8)
                                    .addComponent(totalRam)
                                    .addComponent(lblArquiteturaValue))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 91, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel9)
                                    .addComponent(usoRam, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10)
                                    .addComponent(jLabel7)
                                    .addComponent(cpu)
                                    .addComponent(jLabel1)
                                    .addComponent(lblFabricanteValue)
                                    .addComponent(jLabel3)
                                    .addComponent(grupoDeDisco))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnFechar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblSistemaOperacionalValue)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel5)
                        .addGap(21, 21, 21))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel4)
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(13, 13, 13)
                        .addComponent(lblSistemaOperacionalValue))
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(lblFabricanteValue)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addComponent(lblArquiteturaValue)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(totalRam)
                    .addComponent(usoRam))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cpu)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel10)
                        .addGap(18, 18, 18)
                        .addComponent(grupoDeDisco))
                    .addComponent(btnFechar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnFecharActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFecharActionPerformed
        // TODO add your handling code here:
        try{
            ObjectMapper mapper = new ObjectMapper();
            Localizacao local = mapper.readValue(new URL("https://ipinfo.io/json"), Localizacao.class);
            local.inserirLocalizacao(banco, "S", maquina.getId(), local);
        }
        catch (Exception e){
            System.out.println("Não foi possível inserir a saída");
            System.out.println(e);
        }
        
        System.exit(0);
    }//GEN-LAST:event_btnFecharActionPerformed

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TelaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TelaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TelaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TelaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {

//            public void run() {
//                new TelaPrincipal().setVisible(true);
//            }
//        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFechar;
    private javax.swing.JLabel cpu;
    private javax.swing.JLabel grupoDeDisco;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel lblArquiteturaValue;
    private javax.swing.JLabel lblFabricanteValue;
    private javax.swing.JLabel lblSistemaOperacionalValue;
    private javax.swing.JLabel totalRam;
    private javax.swing.JLabel usoRam;
    // End of variables declaration//GEN-END:variables
}
