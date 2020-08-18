/*
 * 
 *  */
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
	private ArrayList<ArrayList<String>> lista,usuarios;
	private Statement consulta;
	private ResultSet resultado;
	private String sqlConsulta,username,CNPJ;

	// Construtor
	public BuscaCNPJ(Connection conn){

		conexao = conn;
		lista = new ArrayList<ArrayList<String>>();
		usuarios = new ArrayList<ArrayList<String>>();
		setSqlConsulta();
	}

	// Lógica da classe
	@Override
	public void run() {

		ArrayList<String> elemento;

		try{

			consulta = getConexao().createStatement();
			resultado = consulta.executeQuery(sqlConsulta);

			while(resultado.next()){

				elemento = new ArrayList<String>();

				elemento.add(resultado.getString(1));
				elemento.add(resultado.getString(2));

				lista.add(elemento);
			}

			resultado.close();

			pesquisar();

		}catch (SQLException e){
			JOptionPane.showMessageDialog(null,"Não foi possível realizar o SQL!");
		}
	}

	public void pesquisar(){

		// Percorer variável lista, consultando a tabela EMPRESAS em
		// cada usuário do banco.
		String sql;

		for (ArrayList<String> usr: lista){
			sql = "SELECT COUNT (*) FROM "+
					usr.get(0)+
					".EMPRESAS WHERE EMPRESA_ID = '"+
					CNPJ+
					"'";

			try {

				consulta = getConexao().createStatement();
				resultado = consulta.executeQuery(sql);

				// Não mudar a ordem, getInt deve ser chamado após next.
				if (resultado.next() && resultado.getInt(1) > 0){
					usuarios.add(usr);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				JOptionPane.showMessageDialog(null, "Erro ao executar a consulta no usuário "+username+"e CNPJ:"+CNPJ);
			}
		}
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

	public String getCNPJ() {
		return CNPJ;
	}

	public void setCNPJ(String cNPJ) {
		CNPJ = cNPJ;
	}

	public ArrayList<ArrayList<String>> getUsuarios() {
		return usuarios;
	}

	public void setUsuarios(ArrayList<ArrayList<String>> usuarios) {
		this.usuarios = usuarios;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
