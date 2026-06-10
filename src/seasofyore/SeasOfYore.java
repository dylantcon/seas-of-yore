package seasofyore;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
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
import javax.swing.SwingUtilities;
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

    // Navigation panels. The battle-setup screen can express every local
    // matchup (hot-seat, solo vs any AI tier, AI-vs-AI spectating, Classic or
    // SALVO), so it IS the mode selection; the old per-mode screens are gone.
    private JPanel titlePanel;
    private JPanel battleSetupPanel;

    // menu spacing insets, mirroring the javarominoes menu layout: a packed
    // GridBagLayout column where the gaps come from insets, not struts
    private static final Insets HEADER_P = new Insets(20, 0, 20, 0);
    private static final Insets STD_P = new Insets(10, 20, 10, 20);
    private static final Insets BACK_P = new Insets(30, 20, 10, 20);

    // the menus' medieval palette
    private static final Color INK = new Color(18, 10, 28);          // near-black plum
    private static final Color GOLD = new Color(232, 201, 124);      // illuminated gold
    private static final Color PARCHMENT = new Color(229, 213, 175); // aged paper
    private static final Color SEA_BLUE = new Color(24, 58, 94);     // deep water
    private static final Color BLOOD_RED = new Color(122, 24, 24);   // war banner

    // fixed button footprint, as in the javarominoes menus: uniform buttons
    // read as one deliberate column instead of a ragged stack
    private static final int BUTTON_W = 280;
    private static final int BUTTON_H = 60;

    // Battle-setup controls (read when the battle is launched)
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
        battleSetupPanel = createBattleSetupPanel();

        // Add screens to main panel
        mainPanel.add(titlePanel, "TitleScreen");
        mainPanel.add(battleSetupPanel, "BattleSetup");

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
     * Creates the title screen panel with the game wordmark and main menu
     * options.
     *
     * @return the title screen panel
     */
    private JPanel createTitlePanel()
    {
        JPanel panel = createMenuColumnPanel();

        JLabel titleLabel = makeChipLabel("Seas of Yore",
            new Font("Serif", Font.BOLD | Font.ITALIC, 60), GOLD, INK);
        JLabel subtitleLabel = makeChipLabel("~ Developed by Dylan Connolly ~",
            new Font("Serif", Font.ITALIC, 18), INK, PARCHMENT);

        // Create buttons. Battle setup covers every playable matchup, so the
        // title goes straight there; networked play keeps a (disabled) seat
        // at the table until it exists.
        JButton playButton = createMenuButton("Set Sail", Color.WHITE, SEA_BLUE);
        JButton multiplayerButton = createMenuButton("Multiplayer (Coming Soon)",
                                                     INK, PARCHMENT);
        JButton quitButton = createMenuButton("Quit to Desktop", PARCHMENT, BLOOD_RED);

        multiplayerButton.setEnabled(false);

        // Add action listeners
        playButton.addActionListener(e -> cardLayout.show(mainPanel, "BattleSetup"));
        quitButton.addActionListener(e -> System.exit(0));

        // Stack everything as one packed, centered column
        gblAdd(panel, titleLabel, 0, HEADER_P);
        gblAdd(panel, subtitleLabel, 1, HEADER_P);
        gblAdd(panel, new JLabel(), 2, STD_P); // breathing room before the buttons
        gblAdd(panel, playButton, 3, STD_P);
        gblAdd(panel, multiplayerButton, 4, STD_P);
        gblAdd(panel, quitButton, 5, STD_P);

        return panel;
    }

    /**
     * Creates the battle-setup screen, where each civilization can be given
     * to a human or any AI tier and the mode chosen. One screen expresses
     * every local matchup: hot-seat, solo against an AI on either side, or
     * an AI-vs-AI battle to spectate, in Classic or SALVO rules.
     *
     * @return the battle-setup panel
     */
    private JPanel createBattleSetupPanel()
    {
        JPanel panel = createMenuColumnPanel();

        JLabel headerLabel = makeChipLabel("Prepare for Battle",
            new Font("Serif", Font.BOLD, 40), GOLD, INK);
        JLabel descriptionLabel = makeChipLabel(
            "Assign each fleet to a human or an AI commander.",
            new Font("Serif", Font.ITALIC, 16), INK, PARCHMENT);

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

        JButton beginButton = createMenuButton("Begin Battle", Color.WHITE, SEA_BLUE);
        JButton backButton = createMenuButton("Back", INK, PARCHMENT);
        beginButton.addActionListener(e -> launchBattle());
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "TitleScreen"));

        gblAdd(panel, headerLabel, 0, HEADER_P);
        gblAdd(panel, descriptionLabel, 1, HEADER_P);
        gblAdd(panel, makeFieldRow("Britons (Player 1):", britonsSelector), 2, STD_P);
        gblAdd(panel, makeFieldRow("Franks (Player 2):", franksSelector), 3, STD_P);
        gblAdd(panel, modeRow, 4, STD_P);
        gblAdd(panel, beginButton, 5, STD_P);
        gblAdd(panel, backButton, 6, BACK_P);

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
     * Reads the battle-setup selections and launches the configured game.
     */
    private void launchBattle()
    {
        PlayerType britons = (PlayerType) britonsSelector.getSelectedItem();
        PlayerType franks = (PlayerType) franksSelector.getSelectedItem();
        boolean salvo = salvoModeButton.isSelected();

        for (Component comp : mainPanel.getComponents())
            if (comp instanceof GameController)
                mainPanel.remove(comp);

        GameController controller = controllerFactory.createCustomController(
            britons, franks, salvo, () -> cardLayout.show(mainPanel, "TitleScreen"));

        String panelName = "GamePanel";
        mainPanel.add(controller, panelName);
        cardLayout.show(mainPanel, panelName);
    }

    /**
     * Creates a transparent panel laid out as one packed GridBagLayout column.
     * Each screen is see-through so the single shared {@link ChoppySeasPanel}
     * behind the card stack supplies the animated backdrop; the screens only
     * contribute their labels and buttons.
     *
     * @return a transparent column panel to host one menu screen's contents
     */
    private JPanel createMenuColumnPanel()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Creates a styled menu button with the shared fixed footprint, in the
     * given colors. Mirrors the javarominoes buildMenuButton helper: uniform
     * geometry from the constants, identity from the colors.
     *
     * @param text the button text
     * @param fg   the text color
     * @param bg   the button face color
     * @return a styled JButton
     */
    private JButton createMenuButton(String text, Color fg, Color bg)
    {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(BUTTON_W, BUTTON_H));
        button.setFont(new Font("Serif", Font.BOLD, 20));
        button.setForeground(fg);
        button.setBackground(bg);
        button.setFocusable(false);
        return button;
    }

    /**
     * Builds an opaque "chip" label: text on its own solid plate, the same
     * device the javarominoes menus use to keep type legible over an animated
     * background regardless of what is moving behind it.
     *
     * @param text the label text
     * @param font the font to render it in
     * @param fg   the text color
     * @param bg   the plate color
     * @return a styled, opaque JLabel
     */
    private JLabel makeChipLabel(String text, Font font, Color fg, Color bg)
    {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(fg);
        label.setBackground(bg);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
        return label;
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
