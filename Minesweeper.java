import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.event.*;
import java.net.URL;
import java.awt.*;
import java.util.*;
import javax.swing.Timer;
import javax.swing.border.LineBorder;

public class Minesweeper implements ActionListener {
    int board_size = 14;
    int number_of_revealed_tiles = 0;
    int no_of_mines = 30;
    JFrame frame;
    JMenuBar menuBar;
    JMenu options, help;
    JMenuItem newGame, exit, contact, rules, about;
    JPanel buttonPanel;
    JButton tiles[][] = new JButton[board_size][board_size];
    ArrayList<Integer> mines = new ArrayList<>(); // indices of all the mines
    int boardState[][] = new int[board_size][board_size];
    boolean is_first_click = true;
    boolean[][] visited = new boolean[board_size][board_size];

    JLabel timerLabel;
    Timer gameTimer;
    int elapsedTime = 0;

    public static void main(String[] args) {
        new Minesweeper();
    }

    Minesweeper() {
        frame = new JFrame("Minesweeper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        initComponents();
        frame.setVisible(true);
    }

    private void initComponents() {
        // Set up the menu bar and menu items
        menuBar = new JMenuBar();
        options = new JMenu("Options");
        help = new JMenu("Help");
        newGame = new JMenuItem("New Game");
        exit = new JMenuItem("Exit");
        contact = new JMenuItem("Contact");
        rules = new JMenuItem("Rules");
        about = new JMenuItem("About");

        newGame.addActionListener(this);
        exit.addActionListener(this);
        contact.addActionListener(this);
        rules.addActionListener(this);
        about.addActionListener(this);

        options.add(newGame);
        options.add(exit);
        help.add(contact);
        help.add(rules);
        help.add(about);

        menuBar.add(options);
        menuBar.add(help);

        frame.setJMenuBar(menuBar);

        // Timer Label
        timerLabel = new JLabel("Time: 0s", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        timerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Create a panel to hold the timer label
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(timerLabel, BorderLayout.CENTER);

        // Add top panel above the button panel
        frame.add(topPanel, BorderLayout.NORTH);

        buttonPanel = new JPanel(new GridLayout(board_size, board_size));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        frame.add(buttonPanel, BorderLayout.CENTER);

        for (int i = 0; i < board_size; i++) {
            for (int j = 0; j < board_size; j++) {
                Color c1 = new Color(255, 255, 204);
                Color c2 = new Color(255, 255, 155);
                tiles[i][j] = new JButton();
                tiles[i][j].setFont(new Font("Serif", Font.BOLD, 44));

                if ((i + j) % 2 == 0)
                    tiles[i][j].setBackground(c1);
                else
                    tiles[i][j].setBackground(c2);

                tiles[i][j].setBorder(new LineBorder(Color.BLACK, 3));

                buttonPanel.add(tiles[i][j]);

                addTilesActionListeners(i, j);
            }

        }

        startTimer();
    }

    private void startTimer() {
        elapsedTime = 0; // Reset time
        timerLabel.setText("Time: 0s     No of tiles left to be revealed: "
                + (board_size * board_size - number_of_revealed_tiles - no_of_mines)); // Reset display

        gameTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                elapsedTime++;
                timerLabel.setText("Time: " + elapsedTime + "s     " + "No of tiles left to be revealed: "
                        + (board_size * board_size - number_of_revealed_tiles - no_of_mines));
            }
        });

        gameTimer.start();
    }

    void playSound(String filename) {
        try {
            // Load the sound file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource(filename));
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void addTilesActionListeners(int row, int col) {
        tiles[row][col].addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (is_first_click) {
                                generate_mines(board_size * row + col);
                                floodfill(row, col, visited);
                                is_first_click = false;
                            }

                else {
                                floodfill(row, col, visited);
                                checkForGameOver(row, col);
                            }

                        }

                else if (SwingUtilities.isRightMouseButton(e)) {
                            if (tiles[row][col].getText() == "🚩")
                                tiles[row][col].setText("");
                            else
                                tiles[row][col].setText("🚩");

                            playSound("flag.wav");
                        }
                    }
                });
    }

    void checkForGameOver(int row, int col) {
        // Loss
        if (mines.contains(board_size * row + col)) {
            playSound("explosion.wav");
            for (int i = 0; i < board_size; i++) {
                for (int j = 0; j < board_size; j++) {
                    if (boardState[i][j] == -1) {
                        tiles[i][j].setText("💥");
                    }
                }
            }
            gameTimer.stop();
            JOptionPane.showMessageDialog(null, "Game Over! \n" + "No of tiles left to be revealed: "
                    + (board_size * board_size - number_of_revealed_tiles - no_of_mines));
            frame.dispose();
            new Minesweeper();
        }

        // Win
        if (number_of_revealed_tiles == (board_size * board_size - mines.size())) {
            gameTimer.stop();
            JOptionPane.showMessageDialog(null, "You Win!" + "\n Time elapsed: " + elapsedTime + "s");
            frame.dispose();
            new Minesweeper();
        }
    }

    void generate_mines(int index_to_remove) {
        for (int i = 0; i < board_size * board_size; i++) {
            mines.add(i);
        }
        Collections.shuffle(mines);
        mines.remove((Integer) index_to_remove);
        mines.subList(no_of_mines, mines.size()).clear();

        Collections.sort(mines);
        setBoardState();
    }

    void setBoardState() {
        // Set the board state for mines = -1
        // The mines array is a 1D array but the boardState is a 2D array so convert the
        // 1D values to 2D values
        // No. of elements in each row * row number + col no
        for (int j = 0; j < board_size; j++) {
            for (int k = 0; k < board_size; k++) {
                if (mines.contains(board_size * j + k))
                    boardState[j][k] = -1;
            }
        }

        // Now set the state for other tiles
        // Every tile has 8 neighbouring tiles
        // [row-1][col-1]
        // [row-1][col]
        // [row-1][col+1]
        // [row][col-1]
        // [row][col+1]
        // [row+1][col-1]
        // [row+1][col]
        // [row+1][col+1]

        for (int row = 0; row < board_size; row++) {
            for (int col = 0; col < board_size; col++) {

                if (boardState[row][col] == -1)
                    continue;

                int count = 0;
                // Top-left
                if (row - 1 >= 0 && col - 1 >= 0) {
                    if (boardState[row - 1][col - 1] == -1)
                        count++;
                }

                // Top
                if (row - 1 >= 0) {
                    if (boardState[row - 1][col] == -1)
                        count++;
                }

                // Top-right
                if (row - 1 >= 0 && col + 1 <= board_size - 1) {
                    if (boardState[row - 1][col + 1] == -1)
                        count++;
                }

                // Left
                if (col - 1 >= 0) {
                    if (boardState[row][col - 1] == -1)
                        count++;
                }

                // Right
                if (col + 1 <= board_size - 1) {
                    if (boardState[row][col + 1] == -1)
                        count++;
                }

                // Bottom-left
                if (row + 1 <= board_size - 1 && col - 1 >= 0) {
                    if (boardState[row + 1][col - 1] == -1)
                        count++;
                }

                // Bottom
                if (row + 1 <= board_size - 1) {
                    if (boardState[row + 1][col] == -1)
                        count++;
                }

                // Bottom-right
                if (row + 1 <= board_size - 1 && col + 1 <= board_size - 1) {
                    if (boardState[row + 1][col + 1] == -1)
                        count++;
                }

                boardState[row][col] = count;
            }
        }
    }

    void floodfill(int row, int col, boolean[][] visited) {
        if (row < 0 || row >= board_size || col < 0 || col >= board_size || boardState[row][col] == -1) {
            return;
        }

        // If the tile is already visited or is a mine, stop
        if (visited[row][col] || boardState[row][col] == -1) {
            return;
        }

        // Mark this tile as visited
        visited[row][col] = true;

        if (boardState[row][col] == 0) {
            tiles[row][col].setText("");
            tiles[row][col].setEnabled(false);
            tiles[row][col].setBackground(Color.WHITE);
            number_of_revealed_tiles++;
        }

        else {
            tiles[row][col].setText(Integer.toString(boardState[row][col])); // Show number
            tiles[row][col].setEnabled(false);
            tiles[row][col].setBackground(Color.WHITE);
            number_of_revealed_tiles++;
            return; // Stop recursion if it's a numbered tile
        }

        floodfill(row - 1, col - 1, visited); // Top-left
        floodfill(row - 1, col, visited); // Top
        floodfill(row - 1, col + 1, visited); // Top-right
        floodfill(row, col - 1, visited); // Left
        floodfill(row, col + 1, visited); // Right
        floodfill(row + 1, col - 1, visited); // Bottom-left
        floodfill(row + 1, col, visited); // Bottom
        floodfill(row + 1, col + 1, visited); // Bottom-right
    }

    public void actionPerformed(ActionEvent evt) {
        switch (evt.getActionCommand()) {
            case "New Game":
                frame.dispose();
                new Minesweeper();
                break;

            case "Exit":
                System.exit(0);
                break;

            case "Contact":
                try {
                    Desktop.getDesktop().browse(new URL("https://twitter.com/SoumyadeepB2001").toURI());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Browser not found");
                }
                break;

            case "Rules":
                JOptionPane.showMessageDialog(null,
                        "1. Objective: Uncover all safe squares without hitting a mine and flag all the mines.  \r\n" +
                                "2. Start: Click on squares; numbers show how many mines are adjacent, or reveal blank spaces.  \r\n"
                                +
                                "3. Flags: Right-click to flag suspected mines, helping you avoid accidental clicks.  \r\n"
                                +
                                "4. Logic: Use the numbers to deduce safe squares and locate mines without guessing.  \r\n"
                                +
                                "5. Win/Lose: You win by clearing all safe squares and flagging all mines; hitting a mine ends the game.");
                break;

            case "About":
                JOptionPane.showMessageDialog(null,
                        "Minesweeper\nVersion: 1.0.1\nProgram written by Soumyadeep Banerjee, MCA");
                break;
        }
    }
}