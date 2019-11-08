package com.mypcrmulti.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import com.codeminders.hidapi.HIDManager;
import com.hidapi.CallbackDeviceChange;
import com.hidapi.DeviceChange;
import com.hidapi.HidClassLoader;

public class MultiUI extends JFrame implements DeviceChange, MouseListener{

	private static final long serialVersionUID = 2L;
	
	private static final int WIDTH = 332, HEIGHT = 266;
	
	private JTextField textSerial[] = new JTextField[8];
	private JLabel buttonRun[] = new JLabel[8];
	
	// button �� disable ó���� �ϰ� �Ǹ� ui �� ���� �������Ƿ�, flag ó���� ����
	private boolean isEnable[] = new boolean[8];
	private JLabel buttonAllRun = null;
	private boolean buttonAllEnable = false;
	
	private ArrayList<String> connectedDevice = new ArrayList<String>();
	
	private URL url_runUp = getClass().getClassLoader().getResource("runUp.jpg");
	private URL url_runDown = getClass().getClassLoader().getResource("runDown.jpg");
	private URL url_allrunUp = getClass().getClassLoader().getResource("allRunUp.jpg");
	private URL url_allrunDown = getClass().getClassLoader().getResource("allRunDown.jpg");
	private URL url_logo = getClass().getClassLoader().getResource("logo.jpg");
	
	private ImageIcon icon_runUp, icon_runDown, icon_allrunUp, icon_allrunDown;
	
	private HIDManager manager = null;
	private CallbackDeviceChange deviceChangeCallback = null;
	
	private static boolean isMac = false;
	private static String pcrPath = "";
	private static String macIconPath = "";
	private static final String JAR_FILE = "DuxCycler.jar";
	
	static{
		if( !HidClassLoader.LoadLibrary() ){
			JOptionPane.showMessageDialog(null, "Not Supported OS.. Exit the Program.");
			System.exit(-1);
		}
		
		String os = System.getProperty("os.name", "win").toLowerCase();
		
		if( os.indexOf("win") != -1 ){
			isMac = false;
		}else if( os.indexOf("mac") != -1 ){
			isMac = true;
		}
		
		if( !isMac )
			pcrPath = "C:\\mPCR";
		else{
			String classPath = System.getProperty("java.class.path");
			String[] tempPath = classPath.split("/");
			for(int i=0; i<tempPath.length-1; ++i){
				pcrPath += tempPath[i] + "/";
				if( i < tempPath.length-2)
					macIconPath += tempPath[i] + "/";
			}
			
			pcrPath += "mPCR";
			macIconPath += "Resources";
		}
	}
	
	private InputStream getPCRJar(){
		return getClass().getClassLoader().getResourceAsStream(JAR_FILE);
	}
	
	private void makePCRJar(){
		File file = new File(pcrPath);	file.mkdir();
		file = new File(pcrPath + (isMac ? "/" : "\\") + JAR_FILE);
		
		if( !isMac && file.exists() ){
			file.delete();
		}
		
		if( (isMac && !file.exists()) || !isMac ){
			InputStream in = getPCRJar();
			try {
				FileOutputStream out = new FileOutputStream(file);
				int res = -1;
				byte[] buf = new byte[128];
				
				while( (res = in.read(buf)) != -1 )
					out.write(buf, 0, res);
				out.close();
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public MultiUI(){
		// icon ������ �ʱ�ȭ �Ѵ�.
		icon_runUp = new ImageIcon(url_runUp);
		icon_runDown = new ImageIcon(url_runDown);
		icon_allrunUp = new ImageIcon(url_allrunUp);
		icon_allrunDown = new ImageIcon(url_allrunDown);
		
		// title icon ����
		setIconImage(new ImageIcon(getClass().getClassLoader().getResource("icon.png")).getImage());
		
		initUI();
		
		try {
			manager = HIDManager.getInstance();
			deviceChangeCallback = CallbackDeviceChange.getInstance(manager, this);
			deviceChangeCallback.setDaemon(true);
			deviceChangeCallback.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		makePCRJar();
		checker.start();
	}
	
	private void initUI(){
		setSize(WIDTH, HEIGHT);
		setLayout(null);
		setTitle("DuxCycler Lancher V4.0");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		getContentPane().setBackground(new Color(208, 228, 163));
		setLocationRelativeTo(null);
		// set
		
		for(int i=0; i<textSerial.length; ++i){
			textSerial[i] = new JTextField();
			textSerial[i].setBounds(20 + ((i/4)*150), 20 + ((i%4)*35), 105, 25);
			textSerial[i].setEditable(false);
			textSerial[i].setBorder(new EtchedBorder());
			textSerial[i].setHorizontalAlignment(JTextField.CENTER);
			
			buttonRun[i] = new JLabel(icon_runDown);
			buttonRun[i].setBounds(133 + ((i/4)*150), 20 + ((i%4)*35), 25, 25);
			buttonRun[i].addMouseListener(this);
			
			add(textSerial[i]);
			add(buttonRun[i]);
		}
		
		buttonAllRun = new JLabel(icon_allrunDown);
		buttonAllRun.setBounds(220, 160, 88, 26);
		buttonAllRun.addMouseListener(this);
		
		JLabel labelLogo = new JLabel(new ImageIcon(url_logo));
		labelLogo.setBounds(74, 185, 182, 50);
		
		add(buttonAllRun);
		add(labelLogo);
		
		setVisible(true);
	}

	@Override
	public void OnMessage(int MessageType, Object data) {
		// ��� edit text �� ����, ��ư�� ��Ȱ��ȭ ���·� �д�.
		buttonAllRun.setIcon(icon_allrunDown);
		
		for(int i=0; i<textSerial.length; ++i){
			textSerial[i].setText("");
			buttonRun[i].setIcon(icon_runDown);
		}
		
		connectedDevice.clear();
		
		if( data == null ){
			// isEnable �� button �� enable �뵵�� ����Ѵ�.
			// ��ġ�� ���� ��� false �� �ʱ�ȭ �����ش�.
			Arrays.fill(isEnable, false);
		}else{
			String[] serials = (String[])data;
			
			for(int i=0; i<serials.length; ++i){
				isEnable[i] = true;
				textSerial[i].setText(serials[i]);
				buttonRun[i].setIcon(icon_runUp);
				connectedDevice.add(serials[i]);
			}
			
			// allrun button Ȱ��ȭ
			buttonAllRun.setIcon(icon_allrunUp);
			buttonAllEnable = true;
		}
	}
	
	public void mouseClicked(MouseEvent e) {
		Object o = e.getSource();
		
		for(int i=0; i<buttonRun.length; ++i){
			if( buttonRun[i] == o && isEnable[i] ){
				isEnable[i] = false;
				runPCR(connectedDevice.get(i));
			}
		}
		
		if( o == buttonAllRun && buttonAllEnable ){
			for(int i=0; i<connectedDevice.size(); ++i){
				if( isEnable[i] ){
					runPCR(connectedDevice.get(i));
				}
			}
		}
	}
	
	private void runPCR(String serial){
		String[] command = null;
		
		if( isMac ){
			String[] tempCmd = { 	(System.getProperty("java.home") + "/bin/java"),
									"-Dapple.laf.useScreenMenuBar=true",
									"-Dcom.apple.macos.use-file-dialog-packages=true",
									"-Xdock:name=" + serial,
									"-Xdock:icon=" + macIconPath + "/PCR_Kun.icns",
									"-jar", 
									JAR_FILE, 
									serial} ;
			command = tempCmd;
		}else{
			String[] tempCmd = { 	(System.getProperty("java.home") + "/bin/java"),
					"-jar", 
					JAR_FILE, 
					serial} ;
			command = tempCmd;
		}
		
		ProcessBuilder builder = new ProcessBuilder(command);
		try{
			builder.directory(new File(pcrPath));
			builder.start();
		}catch(IOException e){
			JOptionPane.showMessageDialog(null, "PCR Kun is not working! please check your PCR Kun execution file!");
		}
	}
	
	Thread checker = new Thread(){
		public void run(){
			while(true){
				try{
					Thread.sleep(100);
				}catch(InterruptedException e){
					
				}
				
				ArrayList<String> processList = new ArrayList<String>();
				
				if( !isMac ){
					try {
						Process p = Runtime.getRuntime().exec("tasklist /v /fo list");
						InputStream in = p.getInputStream();
						BufferedReader in2 = new BufferedReader(new InputStreamReader(in));
						String line = null;
						while( (line = in2.readLine()) != null ){
							if( line.contains("â ����:"))
								processList.add(line.substring(10));
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{
					try {
						Process p = Runtime.getRuntime().exec("ps -eaf");
						InputStream in = p.getInputStream();
						BufferedReader in2 = new BufferedReader(new InputStreamReader(in));
						String line = null;
						while( (line = in2.readLine()) != null ){
							String[] splits = line.split(" ");
								processList.add(splits[splits.length-1]);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				int cnt = 0;
				for(int i=0; i<connectedDevice.size(); ++i){
					boolean check = !processList.contains(connectedDevice.get(i));
					
					isEnable[i] = check;
					// false �� �����ϴ� ���
					if( !check ){
						buttonRun[i].setIcon(icon_runDown);
						cnt++;
					}else
						buttonRun[i].setIcon(icon_runUp);
				}
				
				if( cnt == connectedDevice.size() ){
					buttonAllRun.setIcon(icon_allrunDown);
					buttonAllEnable = false;
				}else{
					buttonAllRun.setIcon(icon_allrunUp);
					buttonAllEnable = true;
				}
			}
		}
	};
	
	// Not used
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
