package editor;

import java.awt.Image;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;
import pacman.Game;

public class Control {

	View view = new editor.View();
	Model model = new editor.Model();

	public static void main(String[] args) {
		new Control();
	}

	public Control() {
		setDragAndDrop();
		registerActionListeners();
	}

	private void setDragAndDrop() {
		for (JLabel label : view.dnd) {
			label.setTransferHandler(new TransferHandler("icon"));
			label.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					JLabel label = (JLabel) e.getSource();
					model.setActualIcon(label.getIcon());
					label.getTransferHandler().exportAsDrag(label, e, TransferHandler.COPY);
				}
			});
			MyDropTargetListener dtl = new MyDropTargetListener(label);
			DropTarget dt = new DropTarget(label, dtl);
			dt.setDefaultActions(DnDConstants.ACTION_COPY);
			dt.setActive(true);
		}

		for (JLabel label : view.labels) {
			MyDropTargetListener dtl = new MyDropTargetListener(label);
			DropTarget dt = new DropTarget(label, dtl);
			dt.setDefaultActions(DnDConstants.ACTION_COPY);
			dt.setActive(true);
			label.setTransferHandler(new TransferHandler("icon"));
			label.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					JLabel label = (JLabel) e.getSource();
					model.setActualIcon(label.getIcon());
					label.getTransferHandler().exportAsDrag(label, e, TransferHandler.COPY);
					ImageIcon image = new ImageIcon("pacman/images/gray.png");
					image.setImage(image.getImage().getScaledInstance(38, 38, Image.SCALE_DEFAULT));
					label.setIcon(image);
				}
			});
		}
	}

	private void registerActionListeners() {
		view.btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean written = false;
				Object[] options = { "Yes", "No" };
				int result = JOptionPane.showOptionDialog(null, "Would you like to save your level?", "Save", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if (result == JOptionPane.YES_OPTION) {
					writeToFile(view.levelname.getText());
					written = true;
				}
				if (written) {
					result = JOptionPane.showOptionDialog(null, "Would you like to start a new game in your level?", "New Game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
					if (result == JOptionPane.YES_OPTION) {
						try {
							view.dispose();
							new Game("siris.javainterface/src/maps/" + view.levelname.getText() + ".txt");
						} catch (Exception exc) {
							exc.printStackTrace();
						}
					} else {
						System.exit(0);
					}
				}
			}
		});
		view.btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object[] options = { "Yes", "No" };
				int result = JOptionPane.showOptionDialog(null, "Would you really like to cancel?", "Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if (result == JOptionPane.YES_OPTION) {
					System.exit(0);
				}
			}
		});
	}

	private boolean writeToFile(String filename) {
		try {
			FileWriter outFile = new FileWriter(new File("siris.javainterface/src/maps/" + filename + ".txt"));
			PrintWriter out = new PrintWriter(outFile);

			StringBuilder result = new StringBuilder();
			int counter = 0;
			for (JLabel label : view.labels) {
				String icon = label.getIcon().toString();
				if (counter < 15) {
					if (icon.equals("pacman/images/gray.png"))
						result.append(" ");
					else if (icon.equals("pacman/images/line.jpg"))
						result.append("I");
					else if (icon.equals("pacman/images/lineHorizontal.jpg"))
						result.append("-");
					else if (icon.equals("pacman/images/pacman.png"))
						result.append("P");
					else if (icon.equals("pacman/images/ghost.jpg"))
						result.append("G");
					else
						result.append("X");
					counter++;
				}
				if (counter == 15) {
					result.append("\n");
					counter = 0;
				}
			}

			String s = result.toString();
			int pacmanCount = 0;

			Pattern pacman = Pattern.compile(Pattern.quote("P"));
			Matcher p = pacman.matcher(s);
			while (p.find()) {
				pacmanCount++;
			}

			if (pacmanCount != 1) {
				JOptionPane.showMessageDialog(null, "There has to be exactly one Pacman!", "Pacman Error", JOptionPane.ERROR_MESSAGE);
				out.close();
				return false;
			} else {
				out.println(result.toString());
				out.close();
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	// --------------------------------------------

	class MyDropTargetListener implements DropTargetListener {

		JLabel label;

		public MyDropTargetListener(JLabel label) {
			this.label = label;
		}

		public void dragEnter(DropTargetDragEvent dtde) {
		}

		public void dragExit(DropTargetEvent dtde) {
		}

		public void dragOver(DropTargetDragEvent dtde) {
		}

		public void drop(DropTargetDropEvent dtde) {
			if (!view.dnd.contains(label)) {
				ImageIcon image = new ImageIcon(model.getActualIcon().toString());
				image.setImage(image.getImage().getScaledInstance(38, 38, Image.SCALE_DEFAULT));
				label.setIcon(image);
			}
		}

		public void dropActionChanged(DropTargetDragEvent dtde) {
		}
	}

}
