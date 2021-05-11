/* 
 * Classe responsável por executar as ações
 * de monitoramento do BD.
 * 
 * Implementar: 1. Busca de bases.
 * 				2. Gravação de preferências de conexão.
 * 
 * */
package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.System;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.JOptionPane;

public class BancoIF implements Runnable {

	// Variáveis de controle.
	//private PreparedStatement sql;
	private static String nomeDaAplicacao = "Curioso.exe";
	private Statement consulta,consultaModulos,consultaUsuariosOS;
	private ResultSet resultado,resultadoModulos,resultadoUsuariosOS;
	private TreeSet<String> consultas;
	private LinkedBlockingQueue<String> sqlFilaText;
	private Vector<String> modulos,usuariosOS;
	private String sqlConsulta,sqlModulos,sqlUsuariosOS;
	private String porta,servico;
	private String usuario,servidor,senha,nomeComputador,nomeModulo,usuarioOS;
	private Connection conexao;
	private boolean gravar;
	
	// Construtor
	public BancoIF(){
		inicializar();
	}

	public BancoIF(LinkedBlockingQueue<String> filaPrincipal){
		this.sqlFilaText = filaPrincipal;
		inicializar();
	}

	// Inicializar componentes
	public void inicializar(){

		porta="1521";
		servico="ORCL";
		consultas = new TreeSet<String>();
		modulos = new Vector<String>();
		usuariosOS = new Vector<String>();
		gravar = false;

		try {

			nomeComputador = InetAddress.getLocalHost().getHostName();
			nomeComputador.toUpperCase();
			usuarioOS = System.getProperty("user.name").toUpperCase();

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, "Não foi possível obter o nome da máquina ou do usuário.");
		}

	}

	// Getters e Setters
	public void setSql() {

		
		sqlConsulta = "SELECT TRIM(U.SQL_FULLTEXT) "+
				"FROM V$SESSION V, V$SQL U "+
				"WHERE UPPER(V.USERNAME) = '"+usuario+"' "+
				"AND V.SQL_ID = U.sql_id "+
				"AND UPPER(V.PROGRAM) = '"+nomeModulo+"' "+
				"AND UPPER(V.STATUS) = 'ACTIVE' "+
				"AND (UPPER(V.TERMINAL) LIKE '%"+nomeComputador+"' "+
				"OR UPPER(V.MACHINE) LIKE '%"+nomeComputador+"') "+
				"AND UPPER(V.OSUSER) LIKE '%"+usuarioOS+"' "+
				"AND UPPER(USERNAME) NOT IN ('SAC_CIS','SAC_SUPORTE','JUMANJI','CIS_BH','ZANK') "+
				" ORDER BY U.LAST_ACTIVE_TIME";
	}

	public void setSqlModulos(){

		sqlModulos = "SELECT DISTINCT UPPER(V.PROGRAM) "+
				"FROM V$SESSION V "+
				"WHERE UPPER(V.USERNAME) = '"+usuario+"' "+
				"AND (UPPER(V.TERMINAL) LIKE '%"+nomeComputador+"'"+
				"OR UPPER(V.MACHINE) LIKE '%"+nomeComputador+"') "+
				" AND UPPER(USERNAME) NOT IN ('SAC_CIS','SAC_SUPORTE','JUMANJI','CIS_BH','ZANK')";
	}

	public void setSqlUsuariosOS(){

		sqlUsuariosOS = "SELECT DISTINCT SUBSTR(S.OSUSER, (INSTR(S.OSUSER, '\\', -1) + 1)) AS OS_USER "+
				"FROM V$SESSION S "+
				"WHERE S.USERNAME LIKE '"+usuario+"' "+
				"AND (UPPER(S.MACHINE) LIKE '%"+nomeComputador+"' OR "+
				"UPPER(S.TERMINAL) LIKE '%"+nomeComputador+"')";
	}

	public void setGravar(boolean gravar) {
		this.gravar = gravar;
	}

	public void setModulos() {

		String aux;

		try{

			consultaModulos = getConexao().createStatement();
			resultadoModulos = consultaModulos.executeQuery(sqlModulos);

			while(resultadoModulos.next()){

				aux = resultadoModulos.getString(1);
				if(!aux.toUpperCase().equals(BancoIF.nomeDaAplicacao) && !aux.toUpperCase().equals("JDBC THIN CLIENT")){
					modulos.add(aux);
				}
			}
			resultadoModulos.close();
		}catch(SQLException e){

			JOptionPane.showMessageDialog(null,"Não foi possível obter lista de módulos!");
			JOptionPane.showMessageDialog(null, e.getMessage());

		}catch(Exception e){
			JOptionPane.showMessageDialog(null, "Erro ao preparar o SQL.");
			JOptionPane.showMessageDialog(null, e.getMessage());
		}
	}

	public Vector<String> getModulos () {
		return modulos;
	}

	public Vector<String> getUsuariosOS() {
		
		return usuariosOS;
	}

	public void setUsuariosOS() {
		// Consulta banco e preenche lista
		String aux;

		try{

			consultaUsuariosOS = getConexao().createStatement();
			resultadoUsuariosOS = consultaUsuariosOS.executeQuery(sqlUsuariosOS);

			while(resultadoUsuariosOS.next()){

				aux = resultadoUsuariosOS.getString("OS_USER");
				usuariosOS.add(aux.toUpperCase());
			}
			resultadoUsuariosOS.close();
			
		}catch(SQLException e){

			JOptionPane.showMessageDialog(null,"Não foi possível obter lista de usuários!");
			JOptionPane.showMessageDialog(null, e.getMessage());

		}catch(Exception e){
			
			JOptionPane.showMessageDialog(null, e.getMessage());
		
		}
	}

	public boolean getGravar(){
		return gravar;
	}

	/*
	 * Conecta aplicação com o banco de dados e
	 * realiza as chamadas, internas, aos métodos necessários
	 * para preencher a lista de módulos e lista de usuários.
	 * */
	public void setConexao (String servidor,String usuario,String senha){
		
		String url;
		Properties props = new Properties();

		this.servidor = servidor;
		this.usuario = usuario;
		this.senha = senha;

		props.setProperty("ApplicationName",BancoIF.nomeDaAplicacao);
		props.setProperty("user", this.usuario);
		props.setProperty("password",this.senha);

		// Ip ou nome da estação onde está a base de dados
		url = "jdbc:oracle:thin:@" + this.servidor + ":" + this.porta + ":" + servico;

		if(conexao == null){

			try{

				conexao = DriverManager.getConnection(url,props);
				setSqlModulos();
				setModulos();
				setSqlUsuariosOS();
				setUsuariosOS();
				JOptionPane.showMessageDialog(null, "Conexão estabelecida com sucesso!");

			}catch (SQLException e){
				JOptionPane.showMessageDialog(null, "Não foi possível estabelecer comunicação com o banco!");
				JOptionPane.showMessageDialog(null, e.getMessage());
			}
		}
	}

	public Connection getConexao() {
		return conexao;
	}

	public TreeSet<String> getConsultas() {
		return consultas;
	}

	// Funções de Controle
	public boolean isConnected(){
		return conexao != null;
	}

	public void run(){
		monitorarSQL();
	}

	public void monitorarSQL (){

		try{

			consulta = getConexao().createStatement();

			while(gravar){

				resultado = consulta.executeQuery(sqlConsulta);

				while(resultado.next()){

					sqlFilaText.put(resultado.getString(1));

				}
				resultado.close();	
			}

		}catch (SQLException e){
			JOptionPane.showMessageDialog(null,"Não foi possível realizar o SQL!");
			gravar = false;
			JOptionPane.showMessageDialog(null, e.getMessage());
		}catch(Exception e){
			JOptionPane.showMessageDialog(null, "Erro ao preparar o SQL."+e.getMessage());
			System.out.println(e.getMessage());
			gravar = false;
		}
	}

	public static boolean validaCNPJ(String CNPJ){

		String regex = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\z";
		Pattern pt = Pattern.compile(regex);

		Matcher m = pt.matcher(CNPJ);
		return m.matches();

	}

	public String getUsuario() {
		return usuario;
	}
	
	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public String getUsuarioOS() {
		return usuarioOS;
	}

	public void setUsuarioOS(String usuarioOS) {
		this.usuarioOS = usuarioOS;
	}

	public String getNomeModulo() {
		return nomeModulo;
	}

	public void setNomeModulo(String nomeModulo) {
		
		this.nomeModulo = nomeModulo.toUpperCase();
		
	}
}