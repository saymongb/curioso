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
	private Statement consulta,consultaModulos;
	private ResultSet resultado,resultadoModulos;
	private TreeSet<String> consultas;
	private LinkedBlockingQueue<String> sqlFilaText;
	private Vector<String> modulos;
	private String sqlConsulta,sqlresultado,sqlModulos,usuario,servidor,senha;
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
	}

	// Getters e Setters

	public void setSql(String module) {

		String terminal = "%";
		try {
			terminal = InetAddress.getLocalHost().getHostName();
			terminal = terminal.toUpperCase();
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "N�o foi poss�vel obter o nome da esta��o local.");
		}

		module = module.toUpperCase();

		sqlConsulta = "SELECT U.SQL_FULLTEXT "+
				"FROM V$SESSION V, V$SQL U "+
				"WHERE UPPER(V.USERNAME) = '"+usuario+"' "+
				"AND V.SQL_ID = U.sql_id "+
				"AND UPPER(V.PROGRAM) = '"+module+"' "+
				"AND UPPER(V.STATUS) = 'ACTIVE' "+
				"AND UPPER(V.TERMINAL) = '"+terminal+"' "+
				"AND UPPER(V.OSUSER) = '"+System.getProperty("user.name").toUpperCase()+"' "+
				"AND UPPER(USERNAME) NOT IN ('SAC_CIS','SAC_SUPORTE','JUMANJI','CIS_BH','ZANK') "+
				" ORDER BY U.LAST_ACTIVE_TIME";
	}

	public void setSqlModulos(){

		try {

			String terminal;

			terminal = InetAddress.getLocalHost().getHostName();
			terminal = terminal.toUpperCase();

			sqlModulos = "SELECT DISTINCT UPPER(V.PROGRAM) "+
					"FROM V$SESSION V "+
					"WHERE UPPER(V.USERNAME) = '"+usuario+"' "+
					"AND UPPER(V.TERMINAL) = '"+terminal+"' "+
					"AND UPPER(V.OSUSER) = '"+System.getProperty("user.name").toUpperCase()+"'"+
					" AND UPPER(USERNAME) NOT IN ('SAC_CIS','SAC_SUPORTE','JUMANJI','CIS_BH','ZANK')";

		}catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "N�o foi poss�vel obter o nome da esta��o local.");
		}	
	}

	public void setGravar(boolean gravar) {
		this.gravar = gravar;
	}

	public void setModulos() {

		try{

			consultaModulos = getConexao().createStatement();
			resultadoModulos = consultaModulos.executeQuery(sqlModulos);

			while(resultadoModulos.next()){

				modulos.add(resultadoModulos.getString(1));
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

		this.servidor = servidor;
		this.usuario = usuario;
		this.senha = senha;

		// Ip ou nome da esta��o onde est� a base de dados
		url = "jdbc:oracle:thin:@" + servidor + ":" + porta + ":" + servico;

		if(conexao == null){

			try{
				conexao = DriverManager.getConnection(url,usuario,senha);
				JOptionPane.showMessageDialog(null, "Conex�o estabelecida com sucesso!");
				setSqlModulos();
				setModulos();
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

			String regex = "\\p{Digit}{2}\\."+ // xx.
					"\\p{Digit}{3}\\."+ // xxx.
					"\\p{Digit}{3}/"+ // xxx/
					"\\p{Digit}{4}-\\p{Digit}{2}"; // xxxx-xx
			Pattern pt = Pattern.compile(regex);
			Matcher m = pt.matcher(CNPJ);

			return m.matches();
		}

	}