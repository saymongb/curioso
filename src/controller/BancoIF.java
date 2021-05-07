/* 
 * Classe respons�vel por executar as a��es
 * de monitoramento do BD.
 * 
 * Implementar: 1. Busca de bases.
 * 				2. Grava��o de prefer�ncias de conex�o.
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
import javax.swing.JTextArea;

public class BancoIF implements Runnable {

	// Vari�veis de controle.
	//private PreparedStatement sql;
	private static String nomeDaAplicacao = "Curioso.exe";
	private Statement consulta,consultaModulos;
	private ResultSet resultado,resultadoModulos;
	private TreeSet<String> consultas;
	private LinkedBlockingQueue<String> sqlFilaText;
	private Vector<String> modulos;
	private String sqlConsulta,sqlresultado,sqlModulos,sqlUsuariosOS;
	private String usuario,servidor,senha,nomeComputador,usuarioOS;
	private Connection conexao;
	private boolean gravar;
	private JTextArea areaSelect;
	private String comum;

	// Construtor
	public BancoIF(){
		this.comum = comum;
		inicializar();
	}

	public BancoIF(LinkedBlockingQueue<String> filaPrincipal){
		this.sqlFilaText = filaPrincipal;
		inicializar();
	}

	// Inicializar componentes
	public void inicializar(){

		consultas = new TreeSet<String>();
		modulos = new Vector<String>();
		gravar = false;

		try {

			nomeComputador = InetAddress.getLocalHost().getHostName();
			nomeComputador.toUpperCase();
			usuarioOS = System.getProperty("user.name").toUpperCase();

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			JOptionPane.showMessageDialog(null, "N�o foi poss�vel obter o nome da m�quina ou do usu�rio.");
		}

	}

	// Getters e Setters

	public void setSql(String module) {

		module = module.toUpperCase();

		sqlConsulta = "SELECT U.SQL_FULLTEXT "+
				"FROM V$SESSION V, V$SQL U "+
				"WHERE UPPER(V.USERNAME) = '"+usuario+"' "+
				"AND V.SQL_ID = U.sql_id "+
				"AND UPPER(V.PROGRAM) = '"+module+"' "+
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
				"AND UPPER(V.OSUSER) LIKE '%"+usuarioOS+"'"+
				" AND UPPER(USERNAME) NOT IN ('SAC_CIS','SAC_SUPORTE','JUMANJI','CIS_BH','ZANK')";
	}

	public void setSqlUsuariosOS(){

		sqlUsuariosOS = "SELECT DISTINCT SUBSTR(S.OSUSER, (INSTR(S.OSUSER, '\\', -1) + 1)) AS OS_USER "+
				"FROM V$SESSION S "+
				"WHERE S.USERNAME LIKE '"+usuario+"' "+
				"AND (UPPER(S.MACHINE) LIKE '%"+nomeComputador+"' OR "+
				"UPPER(S.TERMINAL) LIKE '%"+nomeComputador+"')";
		System.out.println(sqlUsuariosOS);

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
				//modulos.add(resultadoModulos.getString(1));
			}
			resultadoModulos.close();
		}catch(SQLException e){

			JOptionPane.showMessageDialog(null,"N�o foi poss�vel obter lista de m�dulos!");
			JOptionPane.showMessageDialog(null, e.getMessage());

		}catch(Exception e){
			JOptionPane.showMessageDialog(null, "Erro ao preparar o SQL.");
		}
	}

	public Vector<String> getModulos () {
		return modulos;
	}

	public boolean getGravar(){
		return gravar;
	}

	public void setConexao (String servidor,String usuario,String senha){

		String url,porta="1521",servico="ORCL";
		Properties props = new Properties();

		this.servidor = servidor;
		this.usuario = usuario;
		this.senha = senha;

		props.setProperty("ApplicationName",BancoIF.nomeDaAplicacao);
		props.setProperty("user", usuario);
		props.setProperty("password",senha);

		// Ip ou nome da esta��o onde est� a base de dados
		url = "jdbc:oracle:thin:@" + servidor + ":" + porta + ":" + servico;

		if(conexao == null){

			try{

				//conexao = DriverManager.getConnection(url,usuario,senha);
				conexao = DriverManager.getConnection(url,props);
				setSqlModulos();
				setModulos();
				JOptionPane.showMessageDialog(null, "Conex�o estabelecida com sucesso!");

			}catch (SQLException e){
				JOptionPane.showMessageDialog(null, "N�o foi poss�vel estabelecer comunica��o com o banco!");
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

	public void setAreaSelect(JTextArea areaSelect) {
		this.areaSelect = areaSelect;
	}

	// Fun��es de Controle
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
			JOptionPane.showMessageDialog(null,"N�o foi poss�vel realizar o SQL!");
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

}