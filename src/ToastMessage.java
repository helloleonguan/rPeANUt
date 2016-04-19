import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * ToastMessage - a dialog that pops up for a short period of time to 
 * tell the user something.
 * @author Eric McCreath 2016
 */

public class ToastMessage extends JDialog implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	
	public ToastMessage(JFrame parent, String message, Font font, int time) {
		super(parent, "", true);
		Rectangle parentBounds = parent.getBounds();
		this.setLocation(parentBounds.x + parentBounds.width/3, parentBounds.y+parentBounds.height/3);
		JPanel panel = new JPanel();
		JLabel label = new JLabel(message);
		label.setFont(font);
		panel.add(label);
		getContentPane().add(panel);
		Timer timer = new Timer(time, this);
		timer.setRepeats(false);
		timer.start();
		pack();
		setVisible(true);
	}

	// when the timer goes off the message is simply disposed 
	public void actionPerformed(ActionEvent evt) {
		this.setVisible(false);
		this.dispose();
	}
}
