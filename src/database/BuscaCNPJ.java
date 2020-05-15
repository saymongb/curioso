/*
 * Implementar consulta em paralelo pois pode demorar.
 * */
package database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JOptionPane;

public class BuscaCNPJ implements Runnable{

	// Variáveis de controle
	private Connection conexao;
	private ArrayList<ArrayList<String>> lista;
	private Statement consulta;
	private ResultSet resultado;
	private String sqlConsulta;

	// Construtor
	public BuscaCNPJ(Connection conn){
		
		conexao = conn;
		lista = new ArrayList<ArrayList<String>>();
		setSqlConsulta();

	}
	
	// Métodos de acesso.
	public Connection getConexao() {
		return conexao;
	}

	public void setConexao(Connection conexao) {
		this.conexao = conexao;
	}

	public String getSqlConsulta() {
		return sqlConsulta;
	}

	public void setSqlConsulta() {
		
		this.sqlConsulta = "SELECT DISTINCT A.OWNER AS USUARIO, "+
				"TO_CHAR(AU.created,'DD/MM/YYYY') AS DATA "+
				"FROM ALL_TAB_COLS A, ALL_USERS AU "+
				"WHERE A.TABLE_NAME LIKE 'EMPRESAS' "+
				"AND A.OWNER = AU.username";
	}

	// Lógica da classe
	@Override
	public void run() {
			
			try{

				consulta = getConexao().createStatement();
				resultado = consulta.executeQuery(sqlConsulta);

				while(resultado.next()){

					System.out.println(resultado.getString(1));
					System.out.println(resultado.getString(2));
				}
				
				resultado.close();	
				
			}catch (SQLException e){
				JOptionPane.showMessageDialog(null,"Não foi possível realizar o SQL!");
				e.printStackTrace();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
}
