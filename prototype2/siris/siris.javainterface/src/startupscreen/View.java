package startupscreen;

import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class View extends JFrame {

    public JList levelList;
    public DefaultListModel listModel;
    public JButton btnPlay = new JButton("Play");
    public JButton btnCreateLevel = new JButton("Create Level");
    public JButton btnClose = new JButton("Quit");
    public JTextField gameSpeed = new JTextField(2);

    public View() {
        JPanel selection = new JPanel();
        JPanel main = new JPanel();
        GridLayout grid = new GridLayout(1, 2);
        MigLayout left = new MigLayout("", "", "");
        MigLayout right = new MigLayout("", "[]0[]", "");

        setTitle("Welcome to Pacman");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        getContentPane().setLayout(grid);
        getContentPane().add(selection);
        getContentPane().add(main);

        main.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        selection.setBorder(BorderFactory.createLineBorder(Color.black, 1));

        selection.setLayout(left);
        main.setLayout(right);

        JLabel lblChooseALevel = new JLabel("Choose a Level:");
        lblChooseALevel.setFont(new Font("Dialog Bold", Font.BOLD, 14));
        selection.add(lblChooseALevel, new CC().wrap().gapBottom("20px"));

        listModel = new DefaultListModel();

        File[] fileList = new File("siris.javainterface/src/maps/").listFiles();
        if (fileList != null) {
            if (fileList.length != 0) {
                for (File f : fileList) {
                    String filename = f.getName();
                    if (!filename.startsWith("."))
                        listModel.addElement(filename.substring(0, filename.length()-4));
                }
            }
        }

        levelList = new JList(listModel);
        levelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        levelList.setLayoutOrientation(JList.HORIZONTAL_WRAP);

        if (!listModel.isEmpty())
            levelList.setSelectedIndex(0);

        JScrollPane listScroller = new JScrollPane(levelList);
        listScroller.setPreferredSize(new Dimension(365, 475));

        selection.add(listScroller);

        ImageIcon image = new ImageIcon("pacman/images/pacman.png");
        image.setImage(image.getImage().getScaledInstance(365, 350, Image.SCALE_DEFAULT));

        JLabel lblPacmanPicture = new JLabel(image);
        main.add(lblPacmanPicture, new CC().wrap().gapBottom("20px").cell(0, 0, 2, 1));

        JLabel lblGameSpeed = new JLabel("Game Speed: ");
        main.add(lblGameSpeed);

        lblGameSpeed.setFont(new Font("Dialog Bold", Font.BOLD, 12));
        main.add(lblGameSpeed, new CC().gapBottom("20px"));
        gameSpeed.setText("1");
        main.add(gameSpeed, new CC().wrap());

        main.add(btnPlay, new CC().wrap().width("365!").span(2));
        main.add(btnCreateLevel, new CC().wrap().width("365!").span(2));
        main.add(btnClose, new CC().width("365!").span(2));

        setVisible(true);
    }

}
