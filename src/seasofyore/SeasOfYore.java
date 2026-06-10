package seasofyore;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import seasofyore.core.PlayerType;
import seasofyore.ui.ChoppySeasPanel;

/**
 * Main entry point of the program. Medieval Battleship, using Swing Components.
 * 
 * @author dylan
 */
public class SeasOfYore
{
    // Main application components
    private JFrame mainFrame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    // Navigation panels
    private JPanel titlePanel;
    private JPanel modeSelectionPanel;
    private JPanel aiDifficultyPanel;
    private JPanel customBattlePanel;

    // menu spacing insets, mirroring the javarominoes menu layout: a packed
    // GridBagLayout column where the gaps come from insets, not struts
    private static final Insets STD_P = new Insets(10, 20, 10, 20);
    private static final Insets BACK_P = new Insets(30, 20, 10, 20);

    // Custom-battle controls (read when the battle is launched)
    private JComboBox<PlayerType> britonsSelector;
    private JComboBox<PlayerType> franksSelector;
    private JRadioButton classicModeButton;
    private JRadioButton salvoModeButton;
    
    // Factory for creating game controllers
    private final GameControllerFactory controllerFactory;
    
    private SeasOfYore()
    {
        controllerFactory = new GameControllerFactory();
        initializeUI();
    }
    
    /**
     * Initializes the main UI components and navigation screens.
     */
    private void initializeUI() 
    {
        // Create main application frame
        mainFrame = new JFrame("Seas of Yore");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 800);
        
        // Set up card layout for screen management. The card stack is fully
        // transparent so the animated seas behind it show through every screen.
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setOpaque(false);

        // Create navigation screens
        titlePanel = createTitlePanel();
        modeSelectionPanel = createModeSelectionPanel();
        aiDifficultyPanel = createAIDifficultyPanel();
        customBattlePanel = createCustomBattlePanel();

        // Add screens to main panel
        mainPanel.add(titlePanel, "TitleScreen");
        mainPanel.add(modeSelectionPanel, "ModeSelection");
        mainPanel.add(aiDifficultyPanel, "AIDifficulty");
        mainPanel.add(customBattlePanel, "CustomBattle");
        
        // Compose the whole menu system as a transparent stack floating over one
        // shared, self-animating "choppy seas" background -- the same idea as the
        // javarominoes parallax menus. A JLayeredPane lets us pin the animated
        // panel to the back (DEFAULT_LAYER) and the interactive cards to the
        // front (PALETTE_LAYER). JLayeredPane has no layout manager, so we size
        // both children to fill the frame on every (re)layout.
        ChoppySeasPanel seas = new ChoppySeasPanel();

        JLayeredPane layeredRoot = new JLayeredPane()
        {
            @Override
            public void doLayout()
            {
                for (Component child : getComponents())
                    child.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        layeredRoot.add(seas, JLayeredPane.DEFAULT_LAYER);
        layeredRoot.add(mainPanel, JLayeredPane.PALETTE_LAYER);

        mainFrame.setContentPane(layeredRoot);
    }
    
    /**
     * Shows the application window.
     */
    private void show() 
    {
        cardLayout.show(mainPanel, "TitleScreen");
        mainFrame.setVisible(true);
    }
    
    /**
     * Creates the title screen panel with game logo and main menu options.
     * 
     * @return the title screen panel
     */
    private JPanel createTitlePanel() 
    {
        JPanel panel = createBackgroundPanel();
        panel.setLayout(new BorderLayout());
        
        // Title label
        JLabel titleLabel = new JLabel("Seas of Yore", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD + Font.ITALIC, 60));
        titleLabel.setForeground(Color.MAGENTA);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(0, 0, 50, 0)); // Add some bottom padding

        // Create buttons
        JButton playButton = createMenuButton("Play Game");
        JButton quitButton = createMenuButton("Quit");

        // Add action listeners
        playButton.addActionListener(e -> cardLayout.show(mainPanel, "ModeSelection"));
        quitButton.addActionListener(e -> System.exit(0));

        // Add buttons as a packed, centered column
        gblAdd(buttonPanel, playButton, 0, STD_P);
        gblAdd(buttonPanel, quitButton, 1, STD_P);
        
        // Add components to main panel
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the game mode selection panel.
     * 
     * @return the mode selection panel
     */
    private JPanel createModeSelectionPanel() 
    {
        JPanel panel = createBackgroundPanel();
        panel.setLayout(new BorderLayout());
        
        // Header label
        JLabel headerLabel = new JLabel("Select Game Mode", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Serif", Font.BOLD, 40));
        headerLabel.setForeground(Color.WHITE);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(30, 0, 50, 0));

        // Create buttons
        JButton singlePlayerButton = createMenuButton("Single Player (vs AI)");
        JButton classicButton = createMenuButton("Classic Game");
        JButton salvoButton = createMenuButton("SALVO Game");
        JButton customButton = createMenuButton("Custom Battle");
        JButton lanButton = createMenuButton("LAN Multiplayer");
        JButton onlineButton = createMenuButton("Online Multiplayer");
        JButton backButton = createMenuButton("Back");

        // Set disabled state for unimplemented features
        lanButton.setEnabled(false);
        onlineButton.setEnabled(false);

        // Add action listeners
        singlePlayerButton.addActionListener(e -> cardLayout.show(mainPanel, "AIDifficulty"));
        classicButton.addActionListener(e -> launchGameMode(GameMode.CLASSIC));
        salvoButton.addActionListener(e -> launchGameMode(GameMode.SALVO));
        customButton.addActionListener(e -> cardLayout.show(mainPanel, "CustomBattle"));
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "TitleScreen"));

        // Add buttons as a packed, centered column
        gblAdd(buttonPanel, singlePlayerButton, 0, STD_P);
        gblAdd(buttonPanel, classicButton, 1, STD_P);
        gblAdd(buttonPanel, salvoButton, 2, STD_P);
        gblAdd(buttonPanel, customButton, 3, STD_P);
        gblAdd(buttonPanel, lanButton, 4, STD_P);
        gblAdd(buttonPanel, onlineButton, 5, STD_P);
        gblAdd(buttonPanel, backButton, 6, BACK_P);
        
        // Add components to main panel
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the AI difficulty selection panel.
     * 
     * @return the AI difficulty panel
     */
    private JPanel createAIDifficultyPanel() 
    {
        JPanel panel = createBackgroundPanel();
        panel.setLayout(new BorderLayout());
        
        // Header label
        JLabel headerLabel = new JLabel("Select AI Difficulty", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Serif", Font.BOLD, 40));
        headerLabel.setForeground(Color.WHITE);
        
        // Difficulty description
        JLabel descriptionLabel = new JLabel("<html><center>Choose how challenging your AI opponent will be.</center></html>", SwingConstants.CENTER);
        descriptionLabel.setFont(new Font("Serif", Font.PLAIN, 20));
        descriptionLabel.setForeground(Color.WHITE);
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(headerLabel, BorderLayout.NORTH);
        titlePanel.add(descriptionLabel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(30, 0, 50, 0));

        // Create buttons
        JButton easyButton = createMenuButton("Easy");
        JButton mediumButton = createMenuButton("Medium");
        JButton hardButton = createMenuButton("Hard");
        JButton backButton = createMenuButton("Back");
        
        // Add action listeners
        easyButton.addActionListener(e -> launchGameMode(GameMode.AI_EASY));
        mediumButton.addActionListener(e -> launchGameMode(GameMode.AI_MEDIUM));
        hardButton.addActionListener(e -> launchGameMode(GameMode.AI_HARD));
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "ModeSelection"));
        
        // Add buttons as a packed, centered column
        gblAdd(buttonPanel, easyButton, 0, STD_P);
        gblAdd(buttonPanel, mediumButton, 1, STD_P);
        gblAdd(buttonPanel, hardButton, 2, STD_P);
        gblAdd(buttonPanel, backButton, 3, BACK_P);
        
        // Add components to main panel
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the custom-battle configuration screen, where each civilization
     * can be set to a human or any AI tier and the mode chosen, enabling any
     * matchup including AI-vs-AI spectating.
     *
     * @return the custom-battle panel
     */
    private JPanel createCustomBattlePanel()
    {
        JPanel panel = createBackgroundPanel();
        panel.setLayout(new BorderLayout());

        JLabel headerLabel = new JLabel("Custom Battle", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Serif", Font.BOLD, 40));
        headerLabel.setForeground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(30, 0, 50, 0));

        britonsSelector = createPlayerTypeSelector();
        franksSelector = createPlayerTypeSelector();
        // a sensible default matchup: you (Britons) vs a Medium AI
        britonsSelector.setSelectedItem(PlayerType.HUMAN);
        franksSelector.setSelectedItem(PlayerType.AI_MEDIUM);

        classicModeButton = new JRadioButton("Classic", true);
        salvoModeButton = new JRadioButton("SALVO");
        styleRadio(classicModeButton);
        styleRadio(salvoModeButton);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(classicModeButton);
        modeGroup.add(salvoModeButton);

        JPanel modeRow = new JPanel();
        modeRow.setOpaque(false);
        modeRow.add(classicModeButton);
        modeRow.add(salvoModeButton);

        JButton beginButton = createMenuButton("Begin Battle");
        JButton backButton = createMenuButton("Back");
        beginButton.addActionListener(e -> launchCustomBattle());
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "ModeSelection"));

        gblAdd(form, makeFieldRow("Britons (Player 1):", britonsSelector), 0, STD_P);
        gblAdd(form, makeFieldRow("Franks (Player 2):", franksSelector), 1, STD_P);
        gblAdd(form, modeRow, 2, STD_P);
        gblAdd(form, beginButton, 3, STD_P);
        gblAdd(form, backButton, 4, STD_P);

        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates a combo box listing the player types (human and each AI tier),
     * rendered with their friendly labels.
     *
     * @return a configured player-type selector
     */
    private JComboBox<PlayerType> createPlayerTypeSelector()
    {
        JComboBox<PlayerType> box = new JComboBox<>(PlayerType.values());
        box.setFont(new Font("Serif", Font.BOLD, 18));
        box.setMaximumSize(box.getPreferredSize());
        box.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list,
                Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlayerType)
                    setText(((PlayerType) value).getLabel());
                return this;
            }
        });
        return box;
    }

    /**
     * Builds a labelled row pairing a description with a selector control.
     *
     * @param labelText the field label
     * @param control   the control to pair with it
     * @return a centered row panel
     */
    private JPanel makeFieldRow(String labelText, java.awt.Component control)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Serif", Font.BOLD, 18));
        label.setForeground(Color.WHITE);
        row.add(label);
        row.add(control);
        return row;
    }

    /**
     * Applies the menu's visual style to a radio button.
     *
     * @param button the radio button to style
     */
    private void styleRadio(JRadioButton button)
    {
        button.setFont(new Font("Serif", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setOpaque(false);
    }

    /**
     * Adds a component to a GridBagLayout panel as one row of a packed,
     * centered column. With every weight at zero the column hugs the middle
     * of the panel and the insets alone control the gaps -- the same
     * approach as the javarominoes menus, so buttons stay close together
     * no matter how much room the window has.
     *
     * @param panel the GridBagLayout panel to add to
     * @param c     the component forming this row
     * @param gY    the row index in the column
     * @param i     the insets supplying the spacing around this row
     */
    private void gblAdd(JPanel panel, Component c, int gY, Insets i)
    {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;  // single packed column
        gbc.gridy = gY; // populate gridY

        gbc.anchor = GridBagConstraints.CENTER; // center each row
        gbc.insets = i; // populate inset with supplied Inset instance

        panel.add(c, gbc);
    }

    /**
     * Reads the custom-battle selections and launches the configured game.
     */
    private void launchCustomBattle()
    {
        PlayerType britons = (PlayerType) britonsSelector.getSelectedItem();
        PlayerType franks = (PlayerType) franksSelector.getSelectedItem();
        boolean salvo = salvoModeButton.isSelected();

        for (Component comp : mainPanel.getComponents())
            if (comp instanceof GameController)
                mainPanel.remove(comp);

        GameController controller = controllerFactory.createCustomController(
            britons, franks, salvo, () -> cardLayout.show(mainPanel, "TitleScreen"));

        String panelName = "GamePanel_Custom";
        mainPanel.add(controller, panelName);
        cardLayout.show(mainPanel, panelName);
    }

    /**
     * Launches a game with the specified mode.
     * Creates a new GameController instance using the factory.
     *
     * @param mode the game mode to launch
     */
    private void launchGameMode(GameMode mode) {
        try {
            // Remove any existing game panels
            for (Component comp : mainPanel.getComponents()) {
                if (comp instanceof GameController) {
                    mainPanel.remove(comp);
                }
            }
            
            // Create a new controller for the selected mode
            GameController controller = controllerFactory.createController(mode, () -> {
                cardLayout.show(mainPanel, "TitleScreen");
            });
            
            // Add and show the new controller
            String panelName = "GamePanel_" + mode.toString();
            mainPanel.add(controller, panelName);
            cardLayout.show(mainPanel, panelName);
            
        } catch (UnsupportedOperationException e) {
            // Handle case where mode is not yet implemented
            // In a real app, we'd show a dialog here
            System.out.println("Game mode not yet implemented: " + mode.getDisplayName());
        }
    }
    
    /**
     * Creates a transparent panel for a menu screen. Each screen is see-through
     * so the single shared {@link ChoppySeasPanel} behind the card stack supplies
     * the animated backdrop; the screens only contribute their labels and buttons.
     *
     * @return a transparent panel to host one menu screen's contents
     */
    private JPanel createBackgroundPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }
    
    /**
     * Creates a styled menu button.
     * 
     * @param text the button text
     * @return a styled JButton
     */
    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Serif", Font.BOLD, 20));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(button.getPreferredSize());
        return button;
    }
    
    /**
     * Application entry point.
     * 
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SeasOfYore game = new SeasOfYore();
            game.show();
        });
    }
}



