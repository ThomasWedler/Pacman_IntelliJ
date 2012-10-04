package startupscreen;

import pacman.Game;
import pacman.Main;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class Control {

    private View view = new View();

    public Control() {
        registerActionListeners();
    }

    private void registerActionListeners() {
        view.btnPlay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String filename = view.levelList.getSelectedValue().toString();
                File file = new File("siris.javainterface/src/maps/" + filename + ".txt");
                try {
                    new Game(file);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                view.dispose();
            }
            });

        view.btnCreateLevel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new editor.Control();
                view.dispose();
            }
        });

        view.btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object[] options = { "Yes", "No" };
                int result = JOptionPane.showOptionDialog(null, "Would you really like to quit?", "Cancel", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
                if (result == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
    }

}
