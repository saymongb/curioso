/* 
 * Interface de comunicação com o usuário.
 * 
 * Corrigir:
 * 	1. Problema em determinadas consultas em que a aplicação
 * 		Situação: validar. 
 * A fazer:
 * 	1. Implementar barra de progresso durante a busca de CNPJ.
 * 	2. Implementar gravação de preferências de conexão via arquivo. 
 * 	3. Alterar o SQL das consultas utilizando a lógica da funcionalidade sessions
 * 		do PL/SQL Developer. =>  select * from v$open_cursor where sid = :sid
 * */
package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.ScrollPaneConstants;

import controller.AreaSQLThread;
import controller.BancoIF;
import database.BuscaCNPJ;
import org.eclipse.wb.swing.FocusTraversalOnArray;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.ListModel;

public class Principal {

	// Variáveis de controle
	private BancoIF bancoIF;
	private Thread bancoIFThread,preencheThread;
	private AreaSQLThread preenchedor;

	/* A variável sqlFilaText centraliza toda a lógica da aplicação
	 * servindo como um buffer comum as classes BancoIF e AreaSQLThread.
	 * Trata-se de um típico problema produtor-consumdidor onde BancoIF
	 * alimenta o buffer com o resultado das consultas no banco e AreaSQLThread
	 * consome desse buffer para preencher a tela.
	 * */
	private LinkedBlockingQueue<String> sqlFilaText;
	//private String modulo;

	// Componentes da tela
	private JFrame Principal;
	private JTextField textUsuario;
	private JTextField textServidor;
	private JPasswordField textSenha;
	private JLabel labelUsuario;
	private JLabel labelSenha;
	private JButton buttonGravar;
	private JLabel labelServidor;
	private JTextArea areaSelect;
	private JButton buttonConnectar;
	private JMenuBar menuBar;
	private JMenu mnNewMenu_1;
	private JList<String> listaModulos, listaUsuariosOS;
	private JScrollPane scrolLista, scrolSelects, scrollUsuariosOS;
	private JLabel labelModulos;
	private DefaultListModel<String> modeloLista;
	private JButton btnAtualizarLista;
	private JButton buttonLimpar;
	private JMenuItem menuItemSobre;
	private JMenuItem menuItemNovaCon;
	private JMenuItem mntmBuscarBase;
	private int location_X,location_Y;
	private Dimension dim;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Principal window = new Principal();
					window.Principal.setVisible(true);
					window.Principal.setResizable(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Principal() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		Principal = new JFrame();
		Principal.setResizable(false);
		Principal.setTitle("Curioso");
		Principal.setBounds(100, 100, 898, 574);
		Principal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Principal.getContentPane().setLayout(null);

		// Para inicializar centralizado na tela
		dim = Toolkit.getDefaultToolkit().getScreenSize();
		location_X = (int)dim.width/2-Principal.getSize().width/2;
		location_Y = (int)dim.height/2-Principal.getSize().height/2;
		Principal.setLocation(location_X,location_Y);

		textUsuario = new JTextField();
		textUsuario.setBounds(10, 57, 187, 17);
		Principal.getContentPane().add(textUsuario);
		textUsuario.setColumns(10);

		labelUsuario = new JLabel("Usuário:");
		labelUsuario.setBounds(10, 32, 187, 14);
		Principal.getContentPane().add(labelUsuario);

		labelSenha = new JLabel("Senha:");
		labelSenha.setBounds(10, 85, 187, 14);
		Principal.getContentPane().add(labelSenha);

		textServidor = new JTextField();
		textServidor.setToolTipText("Nome ou IP da esta\u00E7\u00E3o.");
		textServidor.setColumns(10);
		textServidor.setBounds(10, 163, 187, 17);
		Principal.getContentPane().add(textServidor);

		buttonGravar = new JButton("Gravar consultas");
		buttonGravar.setBounds(10, 225, 187, 23);
		Principal.getContentPane().add(buttonGravar);

		textSenha = new JPasswordField ();
		textSenha.setBounds(10, 110, 187, 17);
		textSenha.setColumns(10);
		Principal.getContentPane().add(textSenha);

		labelServidor = new JLabel("Nome do computador do servidor:");
		labelServidor.setBounds(10, 138, 204, 14);
		Principal.getContentPane().add(labelServidor);

		scrolSelects = new JScrollPane();
		scrolSelects.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrolSelects.setSize(618, 472);
		scrolSelects.setLocation(255, 52);
		Principal.getContentPane().add(scrolSelects);

		buttonConnectar = new JButton("Conectar ao banco");
		buttonConnectar.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				// Implementar gravação nesta ação também.
			}
		});
		buttonConnectar.setBounds(10, 191, 187, 23);
		Principal.getContentPane().add(buttonConnectar);

		menuBar = new JMenuBar();
		menuBar.setBounds(0, 0, 892, 21);

		mnNewMenu_1 = new JMenu("Op\u00E7\u00F5es");

		menuBar.add(mnNewMenu_1);

		mntmBuscarBase = new JMenuItem(new AbstractAction("Buscar base"){

			private static final long serialVersionUID = 2L;

			public void actionPerformed(ActionEvent arg0) {

				String CNPJ = (String)JOptionPane.showInputDialog(null,
						"Informe o CNPJ:",
						"Buscar base",
						JOptionPane.PLAIN_MESSAGE,
						null,
						null,
						"XX.XXX.XXX/XXXX-XX");

				exibirBuscaCNPJ(CNPJ);
			}
		});

		menuItemNovaCon = new JMenuItem(new AbstractAction("Nova Conexão") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 2L;

			public void actionPerformed(ActionEvent e) {
				reiniciar();
			}
		});

		mnNewMenu_1.add(mntmBuscarBase);
		mnNewMenu_1.add(menuItemNovaCon);

		JMenuItem mntmPreferncias = new JMenuItem("Prefer\u00EAncias");
		mnNewMenu_1.add(mntmPreferncias);

		menuItemSobre = new JMenuItem(new AbstractAction("Sobre") {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null,"Contato: suporte@cessistemas.com.br",
						"Release 6.6.6",JOptionPane.INFORMATION_MESSAGE);
			}
		});
		mnNewMenu_1.add(menuItemSobre);
		Principal.getContentPane().add(menuBar);

		scrolLista = new JScrollPane();
		scrolLista.setSize(187, 83);
		scrolLista.setLocation(10, 318);
		modeloLista = new DefaultListModel<String>();
		Principal.getContentPane().add(scrolLista);
		listaModulos = new JList<String>(modeloLista);
		scrolLista.setViewportView(listaModulos);
		listaModulos.setBackground(Color.WHITE);
		listaModulos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listaModulos.setLayoutOrientation(JList.VERTICAL_WRAP);
		listaModulos.setVisibleRowCount(-1);

		// Alterar o módulo no SQL
		listaModulos.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				bancoIF.getConsultas().clear();
				if(listaModulos.getSelectedValue() != null){
					bancoIF.setSql(listaModulos.getSelectedValue());
					areaSelect.setText("");	
				}
			}
		});
		
		labelModulos = new JLabel("M\u00F3dulos conectados:");
		labelModulos.setBounds(10, 293, 135, 14);
		Principal.getContentPane().add(labelModulos);

		btnAtualizarLista = new JButton("Atualizar lista de m\u00F3dulos");
		btnAtualizarLista.setBounds(10, 412, 187, 23);
		btnAtualizarLista.setEnabled(false);
		Principal.getContentPane().add(btnAtualizarLista);

		JLabel lblSql = new JLabel("Resultado");
		lblSql.setBounds(531, 32, 64, 14);
		Principal.getContentPane().add(lblSql);

		buttonLimpar = new JButton("Limpar resultados de SQL");
		buttonLimpar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				areaSelect.setText("");
				bancoIF.getConsultas().clear();
			}
		});
		buttonLimpar.setBounds(10, 259, 187, 23);
		Principal.getContentPane().add(buttonLimpar);

		// Listeners

		// Atualiza Lista de módulos conectados
		btnAtualizarLista.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				bancoIF.getModulos().clear();
				bancoIF.setModulos();
				listaModulos.setListData(bancoIF.getModulos());

			}
		});

		// Inicia-interrompe gravação do SQL
		buttonGravar.addMouseListener(new MouseAdapter() {
			// A lógica deste método está muito complexa, deve ser melhorada.
			@Override
			public void mouseClicked(MouseEvent arg0) {

				bancoIF.setGravar(!(bancoIF.getGravar()));

				if (bancoIF.getGravar()){

					resetarCampos(true);

					if(listaModulos.getSelectedValue() != null){

						if(bancoIF.isConnected()){

							bancoIFThread = new Thread(bancoIF);
							preencheThread = new Thread(preenchedor);

							bancoIFThread.setPriority(Thread.MAX_PRIORITY);
							preencheThread.setPriority(Thread.MAX_PRIORITY);

							bancoIFThread.start();
							preencheThread.start();

						}else{

							JOptionPane.showMessageDialog(null, "Conectar primeiro!");
							resetarCampos(false);
						}
					}else{
						JOptionPane.showMessageDialog(null, "Selecione um módulo primeiro!");
						resetarCampos(false);
					}
				}else{
					resetarCampos(false);
				}
			}
		});

		// Conecta aplicação com o banco
		buttonConnectar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {

				bancoIF.setConexao(textServidor.getText().toUpperCase(), 
						textUsuario.getText().toUpperCase(),
						new String(textSenha.getPassword()));

				if(bancoIF.isConnected()){
					buttonConnectar.setEnabled(false);
					buttonConnectar.enableInputMethods(false);
					buttonConnectar.setText("Conectado.");
					//Preencher lista
					listaModulos.setListData(bancoIF.getModulos());
					btnAtualizarLista.setEnabled(true);
					// Preencher lista de usuários
					bancoIF.setSqlUsuariosOS();
				}
			}
		});

		//Variáveis de controle
		sqlFilaText = new LinkedBlockingQueue<String>();
		bancoIF = new BancoIF(sqlFilaText);

		JLabel lblUsuriosDoSistema = new JLabel("Usu\u00E1rios do sistema operacional:");
		lblUsuriosDoSistema.setBounds(10, 446, 190, 14);
		Principal.getContentPane().add(lblUsuriosDoSistema);

		areaSelect = new JTextArea();
		Principal.getContentPane().add(areaSelect);
		areaSelect.setEditable(false);
		areaSelect.setLineWrap(true);
		areaSelect.setBounds(373, 52, 498, 470);
		areaSelect.setAutoscrolls(true);
		areaSelect.setBorder(BorderFactory.createLineBorder(Color.black));
		lblSql.setLabelFor(areaSelect);
		preenchedor = new AreaSQLThread(sqlFilaText,areaSelect);
		bancoIF.setAreaSelect(areaSelect);

		scrollUsuariosOS = new JScrollPane();
		scrollUsuariosOS.setBounds(10, 471, 190, 53);
		Principal.getContentPane().add(scrollUsuariosOS);
		listaUsuariosOS = new JList<String>(modeloLista);
		scrollUsuariosOS.setViewportView(listaUsuariosOS);
	}

	public void reiniciar (){

		resetarCampos(false);

		sqlFilaText = new LinkedBlockingQueue<String>();
		bancoIF = new BancoIF(sqlFilaText);
		preenchedor = new AreaSQLThread(sqlFilaText,areaSelect);
		bancoIF.setAreaSelect(areaSelect);
		bancoIF.setGravar(false);
		textSenha.setText(null);
		textServidor.setText(null);
		textUsuario.setText(null);
		areaSelect.setText(null);
		bancoIF.getModulos().clear();
		buttonConnectar.setEnabled(true);
		buttonConnectar.enableInputMethods(true);
		buttonConnectar.setText("Conectar!");
		listaModulos.setListData(bancoIF.getModulos());
	}

	public void resetarCampos(boolean gravar){

		if(gravar){
			bancoIF.getConsultas().clear();
			buttonGravar.setText("Interromper");
			btnAtualizarLista.setEnabled(false);
		}else{
			buttonGravar.setText("Gravar");
			bancoIF.setGravar(false);
			btnAtualizarLista.setEnabled(false);
			btnAtualizarLista.enableInputMethods(false);
		}
	}

	public boolean validarCampos(){
		// implementar tipo de validação via Enum
		return true;

	}

	public void exibirBuscaCNPJ(String CNPJ){

		int location_X, location_Y;
		GridLayout meuGrid = new GridLayout();
		JFrame frameResultados = new JFrame();
		int qtdUsuarios;
		JTextField textGridAux;

		if(CNPJ != null && BancoIF.validaCNPJ(CNPJ)){

			if(bancoIF.getConexao() == null){

				JOptionPane.showMessageDialog(null, "Atenção! Necessário conectar ao banco de dados primeiro.");

			}else{
				/// Apenas para teste, implementar validação do formulário.
				BuscaCNPJ aux = new BuscaCNPJ(bancoIF.getConexao());
				aux.setUsername(bancoIF.getUsuario());
				aux.setCNPJ(CNPJ);
				Thread t1 = new Thread(aux);
				t1.start();

				try {

					t1.join();
					qtdUsuarios = aux.getUsuarios().size(); 

					if (qtdUsuarios == 0){
						JOptionPane.showMessageDialog(null, "Não existem usuários no banco com o CNPJ:"+CNPJ);
					}else{

						// Definir leiaute do frame e quantidade de linhas.
						meuGrid.setColumns(2);
						meuGrid.setRows(qtdUsuarios+2);
						frameResultados.getContentPane().setLayout(meuGrid);
						frameResultados.setMinimumSize(new Dimension(300,200));

						// Cabeçalho
						frameResultados.getContentPane().add(new JLabel("Username"));
						frameResultados.getContentPane().add(new JLabel("Data de criação"));

						for (ArrayList<String> dadosUsuario : aux.getUsuarios()){

							// Nome do usuário no banco
							textGridAux = new JTextField(dadosUsuario.get(0));
							textGridAux.setEditable(false);
							frameResultados.getContentPane().add(textGridAux);

							// Data de criação
							textGridAux = new JTextField(dadosUsuario.get(1));
							textGridAux.setEditable(false);
							frameResultados.getContentPane().add(textGridAux);

						}

						frameResultados.setTitle("Resultados:");
						location_X = (int)dim.width/2-frameResultados.getSize().width/2;
						location_Y = (int)dim.height/2-frameResultados.getSize().width/2;
						frameResultados.setLocation(location_X, location_Y);
						frameResultados.setVisible(true);

					}

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else{

			JOptionPane.showMessageDialog(
					null,
					"CNPJ inválido!",
					"Atenção!",
					JOptionPane.ERROR_MESSAGE
					);
		}
	}
}