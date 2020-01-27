/*
 * Esta classe apenas preenche a área de SQL de forma
 * concorrente a execução das consultas no banco.
 * 
 * */

package main;

import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.JTextArea;

public class AreaSQLThread implements Runnable{

	private TreeSet<String> consultasControle;
	private SortedSet<String> consultasBanco;
	private JTextArea areaConsultas;
	private String consultaAtual,temp;

	public AreaSQLThread(SortedSet<String> consultasBanco,
			JTextArea areaConsultas){
		this.consultasBanco = consultasBanco;
		this.areaConsultas = areaConsultas;
		consultasControle = new TreeSet<String>();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true){

			consultaAtual = consultasBanco.first();
			
			System.out.println(consultaAtual);

			if(!consultasControle.contains(consultaAtual)){
				consultasControle.add(consultaAtual);
				temp = "\n<--Inicio de SQL-->\n"+
						consultaAtual+
						"\n<--Fim de SQL.-->";
				areaConsultas.append(temp);
			}
		}

	}
}
