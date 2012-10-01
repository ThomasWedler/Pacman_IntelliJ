package editor;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.LinkedList;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

public class View extends JFrame {
	private static final long serialVersionUID = -2325156572708621182L;

	JFrame window = new JFrame("Pacman - Level Editor");
	JPanel view = new JPanel();
	JPanel items = new JPanel();
	GridLayout grid = new GridLayout(1, 2);
	MigLayout left = new MigLayout("wrap 15", "[]0", "[]0");
	MigLayout right = new MigLayout("", "20[]", "[65!]10");
	
	public JTextField levelname = new JTextField(15);
	public JLabel lblBlankIcon = new JLabel();
	public JLabel lblCrossingIcon = new JLabel();
	public JLabel lblCurveOneIcon = new JLabel();
	public JLabel lblCurveTwoIcon = new JLabel();
	public JLabel lblCurveThreeIcon = new JLabel();
	public JLabel lblCurveFourIcon = new JLabel();
	public JLabel lblVerticalLineIcon = new JLabel();
	public JLabel lblHorizontalLineIcon = new JLabel();
	public JLabel lblTPieceOneIcon = new JLabel();
	public JLabel lblTPieceTwoIcon = new JLabel();
	public JLabel lblTPieceThreeIcon = new JLabel();
	public JLabel lblTPieceFourIcon = new JLabel();
	public JLabel lblPacmanIcon = new JLabel();
	public JLabel lblGhostIcon = new JLabel();
	public JButton btnSave = new JButton("Save");
	public JButton btnCancel = new JButton("Cancel");

	public LinkedList<JLabel> labels = new LinkedList<JLabel>();
	public LinkedList<JLabel> dnd = new LinkedList<JLabel>();

	public View() {
		setTitle("Pacman - Level Editor");
		setSize(1280, 720);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setLocationRelativeTo(null);

		getContentPane().setLayout(grid);
		getContentPane().add(view);
		getContentPane().add(items);

		view.setLayout(left);
		items.setLayout(right);

		view.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		items.setBorder(BorderFactory.createLineBorder(Color.black, 1));

		JLabel lblLevel = new JLabel("Level:");
		lblLevel.setFont(new Font("Dialog Bold", 1, 14));
		view.add(lblLevel, new CC().wrap().gapBottom("20px"));

		ImageIcon image = new ImageIcon("pacman/images/gray.png");
		image.setImage(image.getImage().getScaledInstance(38, 38, Image.SCALE_DEFAULT));

		for (int i = 0; i < 225; i++) {
			JLabel label = new JLabel(image);
			label.setBorder(BorderFactory.createLineBorder(Color.black, 1));
			labels.add(label);
		}

		for (JLabel label : labels) {
			view.add(label);
		}
		
		JLabel lblLevelname = new JLabel("Levelname:");
		lblLevelname.setFont(new Font("Dialog Bold", 1, 14));
		items.add(lblLevelname, new CC().gapBottom("50px"));
		levelname.setText("New Level");
		items.add(levelname, new CC().wrap());
		
		ImageIcon icon = new ImageIcon("pacman/images/gray.png");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblBlank = new JLabel("Blank: ");
		items.add(lblBlank, new CC());
		items.add(lblBlankIcon, new CC());
		lblBlankIcon.setIcon(icon);
		dnd.add(lblBlankIcon);
		
		icon = new ImageIcon("pacman/images/crossing.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblCrossing = new JLabel("Crossing: ");
		items.add(lblCrossing, new CC());
		items.add(lblCrossingIcon, new CC().wrap());
		lblCrossingIcon.setIcon(icon);
		dnd.add(lblCrossingIcon);
		
		icon = new ImageIcon("pacman/images/curve.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));

		JLabel lblCurveOne = new JLabel("Curve One: ");
		items.add(lblCurveOne, new CC());
		items.add(lblCurveOneIcon, new CC());
		lblCurveOneIcon.setIcon(icon);
		dnd.add(lblCurveOneIcon);
		
		icon = new ImageIcon("pacman/images/curveTwo.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblCurveTwo = new JLabel("Curve Two: ");
		items.add(lblCurveTwo, new CC());
		items.add(lblCurveTwoIcon, new CC().wrap());
		lblCurveTwoIcon.setIcon(icon);
		dnd.add(lblCurveTwoIcon);

		icon = new ImageIcon("pacman/images/curveThree.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblCurveThree = new JLabel("Curve Three: ");
		items.add(lblCurveThree, new CC());
		items.add(lblCurveThreeIcon, new CC());
		lblCurveThreeIcon.setIcon(icon);
		dnd.add(lblCurveThreeIcon);

		icon = new ImageIcon("pacman/images/curveFour.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblCurveFour = new JLabel("Curve Four: ");
		items.add(lblCurveFour, new CC());
		items.add(lblCurveFourIcon, new CC().wrap());
		lblCurveFourIcon.setIcon(icon);
		dnd.add(lblCurveFourIcon);

		icon = new ImageIcon("pacman/images/line.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));

		JLabel lblVerticalLine = new JLabel("Vertical Line: ");
		items.add(lblVerticalLine, new CC());
		items.add(lblVerticalLineIcon, new CC());
		lblVerticalLineIcon.setIcon(icon);
		dnd.add(lblVerticalLineIcon);
		
		icon = new ImageIcon("pacman/images/lineHorizontal.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));

		JLabel lblHorizontalLine = new JLabel("Horizontal Line: ");
		items.add(lblHorizontalLine, new CC());
		items.add(lblHorizontalLineIcon, new CC().wrap());
		lblHorizontalLineIcon.setIcon(icon);
		dnd.add(lblHorizontalLineIcon);

		icon = new ImageIcon("pacman/images/t-piece.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));

		JLabel lblTPieceOne = new JLabel("T-Piece One: ");
		items.add(lblTPieceOne, new CC());
		items.add(lblTPieceOneIcon, new CC());
		lblTPieceOneIcon.setIcon(icon);
		dnd.add(lblTPieceOneIcon);

		icon = new ImageIcon("pacman/images/t-pieceTwo.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblTPieceTwo = new JLabel("T-Piece Two: ");
		items.add(lblTPieceTwo, new CC());
		items.add(lblTPieceTwoIcon, new CC().wrap());
		lblTPieceTwoIcon.setIcon(icon);
		dnd.add(lblTPieceTwoIcon);

		icon = new ImageIcon("pacman/images/t-pieceThree.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblTPieceThree = new JLabel("T-Piece Three: ");
		items.add(lblTPieceThree, new CC());
		items.add(lblTPieceThreeIcon, new CC());
		lblTPieceThreeIcon.setIcon(icon);
		dnd.add(lblTPieceThreeIcon);

		icon = new ImageIcon("pacman/images/t-pieceFour.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblTPieceFour = new JLabel("T-Piece Four: ");
		items.add(lblTPieceFour, new CC());
		items.add(lblTPieceFourIcon, new CC().wrap());
		lblTPieceFourIcon.setIcon(icon);
		dnd.add(lblTPieceFourIcon);
		
		icon = new ImageIcon("pacman/images/pacman.png");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblPacman = new JLabel("Pacman: ");
		items.add(lblPacman, new CC());
		items.add(lblPacmanIcon, new CC());
		lblPacmanIcon.setIcon(icon);
		dnd.add(lblPacmanIcon);

		icon = new ImageIcon("pacman/images/ghost.jpg");
		icon.setImage(icon.getImage().getScaledInstance(65, 65, Image.SCALE_DEFAULT));
		
		JLabel lblGhost = new JLabel("Ghost: ");
		items.add(lblGhost, new CC());
		items.add(lblGhostIcon, new CC().wrap());
		lblGhostIcon.setIcon(icon);
		dnd.add(lblGhostIcon);
		
		JPanel bot = new JPanel();
		bot.add(btnSave);
		bot.add(btnCancel);
		items.add(bot, new CC().gapTop("40px").gapLeft("435px").span(4));
		
		setVisible(true);
	}
}
