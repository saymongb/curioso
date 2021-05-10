/*
 * Esta classe apenas preenche a área de SQL de forma
 * concorrente a execução das consultas no banco.
 * 
 * */

package controller;

import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

public class AreaSQLThread implements Runnable{

	private TreeSet<String> consultasControle;
	private LinkedBlockingQueue<String> sqlFilaText;
	private JTextArea areaConsultas;
	private String consultaAtual,temp;
	
	public AreaSQLThread(LinkedBlockingQueue<String> consultasBanco,
			JTextArea areaConsultas){
		this.sqlFilaText = consultasBanco;
		this.areaConsultas = areaConsultas;
		consultasControle = new TreeSet<String>();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true){

			try {
				// Este método bloqueia até que a fila tenha novos elementos.
				consultaAtual = sqlFilaText.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				JOptionPane.showMessageDialog(null, e.getMessage());
			}

			if(consultaAtual != null &&
					!consultasControle.contains(consultaAtual)){
				consultasControle.add(consultaAtual);
				temp = "\n-----Inicio de SQL-----\n"+
						consultaAtual+
						"\n-----Fim de SQL-----\n";
				areaConsultas.append(temp);
				
			}
		}

	}
}
