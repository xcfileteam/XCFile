package commonapplet;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

@SuppressWarnings("serial")
public class FlatButton extends JButton implements MouseListener{
	private Color borderColor;
	private Color noColor;
	private Color hiColor;
	private Color nowColor;
	private JLabel labelText;
	
	public FlatButton(String text){
		super();
		
		borderColor = new Color(206,0,0);
		noColor = new Color(246,34,23);
		hiColor = new Color(255,48,37);
		nowColor = noColor;
		
		this.setLayout(new GridLayout(1,1));
		labelText = new JLabel(text,JLabel.CENTER);
		labelText.setForeground(Color.WHITE);
		labelText.setFont(new Font("Sans Serif",Font.BOLD,15));
		this.add(labelText);
		
		this.addMouseListener(this);
	}
	
	@Override
	public void setText(String text){
		labelText.setText(text);
	}
	
	@Override
	public void paintComponent(Graphics g){
		int width;
		int height;
		
		width = this.getWidth();
		height = this.getHeight();
		g.setColor(nowColor);
		g.fillRect(0,0,width,height);
	}
	
	@Override
	public void paintBorder(Graphics g){
		int width;
		int height;
		
		width = this.getWidth();
		height = this.getHeight();
		g.setColor(borderColor);
		g.drawRect(0,0,width - 1,height - 1);
	}

	@Override
	public void mouseClicked(MouseEvent e){}
	
	@Override
	public void mouseEntered(MouseEvent e){
		nowColor = hiColor;
	}
	
	@Override
	public void mouseExited(MouseEvent e){
		nowColor = noColor;
	}
	
	@Override
	public void mousePressed(MouseEvent e){}
	
	@Override
	public void mouseReleased(MouseEvent e){}
}
