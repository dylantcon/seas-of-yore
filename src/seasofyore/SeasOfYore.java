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
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import seasofyore.core.MatchConfig;
import seasofyore.core.PlayerType;
import seasofyore.core.SavedMatch;
import seasofyore.ui.ChoppySeasPanel;
import seasofyore.ui.SavedMatchDialogs;
import seasofyore.ui.WoodPanel;

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
    private static final Color WOOD_EDGE = new Color(43, 26, 12);    // tarred beam

    // fixed button footprint, as in the javarominoes menus: uniform buttons
    // read as one deliberate column instead of a ragged stack
    private static final int BUTTON_W = 280;
    private static final int BUTTON_H = 60;

    // Battle-setup controls (read when the battle is launched)
    private JComboBox<PlayerType> britonsSelector;
    private JComboBox<PlayerType> franksSelector;
    private javax.swing.JTextField britonsNameField;
    private javax.swing.JTextField franksNameField;
    private JButton classicModeButton;
    private JButton salvoModeButton;
    private JButton stoneToggleButton;
    private JLabel commanderLoreLabel;
    private boolean salvoSelected = false;
    private boolean stoneAnimationsEnabled = true;

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
        JButton loadButton = createMenuButton("Recover a Voyage", INK, PARCHMENT);
        JButton multiplayerButton = createMenuButton("Multiplayer (Coming Soon)",
                                                     INK, PARCHMENT);
        JButton quitButton = createMenuButton("Quit to Desktop", PARCHMENT, BLOOD_RED);

        multiplayerButton.setEnabled(false);

        // Add action listeners
        playButton.addActionListener(e -> cardLayout.show(mainPanel, "BattleSetup"));
        loadButton.addActionListener(e -> loadSavedGame());
        quitButton.addActionListener(e -> System.exit(0));

        // Stack everything as one packed, centered column
        gblAdd(panel, titleLabel, 0, HEADER_P);
        gblAdd(panel, subtitleLabel, 1, HEADER_P);
        gblAdd(panel, new JLabel(), 2, STD_P); // breathing room before the buttons
        gblAdd(panel, playButton, 3, STD_P);
        gblAdd(panel, loadButton, 4, STD_P);
        gblAdd(panel, multiplayerButton, 5, STD_P);
        gblAdd(panel, quitButton, 6, STD_P);

        return panel;
    }

    /**
     * Prompts for a saved-game file and resumes the bottled match. The save
     * carries the whole board -- players, fleets, wounds, AI state, and whose
     * turn it is -- so the controller picks up exactly where the log left off.
     */
    private void loadSavedGame()
    {
        SavedMatch saved = SavedMatchDialogs.loadViaDialog(mainFrame);
        if (saved == null)
            return; // cancelled, or the dialog already reported the failure

        for (Component comp : mainPanel.getComponents())
            if (comp instanceof GameController)
                mainPanel.remove(comp);

        GameController controller = new GameController(saved,
            () -> cardLayout.show(mainPanel, "TitleScreen"));

        mainPanel.add(controller, "GamePanel");
        cardLayout.show(mainPanel, "GamePanel");
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

        // The configuration area is a stretch of ship's deck: the same wood
        // texture the in-game sidebar wears, framed by tarred beams, with
        // every control styled to sit on planks rather than in a dialog box.
        JPanel deck = new WoodPanel();
        deck.setLayout(new GridBagLayout());
        deck.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WOOD_EDGE, 5),
            BorderFactory.createEmptyBorder(16, 26, 16, 26)));

        britonsSelector = createPlayerTypeSelector();
        franksSelector = createPlayerTypeSelector();
        // a sensible default matchup: you (Britons) vs a Medium AI
        britonsSelector.setSelectedItem(PlayerType.HUMAN);
        franksSelector.setSelectedItem(PlayerType.AI_MEDIUM);

        // human commanders sign the muster roll; the game imposes the
        // Commander honorific wherever it speaks of them. Each field sits
        // inline with its side's selector and only appears while a human
        // commands that side.
        britonsNameField = makeNameField("Arthur");
        franksNameField = makeNameField("Charlemagne");

        // rules of engagement: a two-plank toggle instead of radio buttons
        classicModeButton = makeDeckToggle("Classic", 120);
        salvoModeButton = makeDeckToggle("SALVO", 120);
        classicModeButton.addActionListener(e -> { salvoSelected = false; refreshModeToggle(); });
        salvoModeButton.addActionListener(e -> { salvoSelected = true; refreshModeToggle(); });

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        modeRow.setOpaque(false);
        modeRow.add(makeDeckLabel("Rules of Engagement:"));
        modeRow.add(classicModeButton);
        modeRow.add(salvoModeButton);

        // stone volleys: whether attacks ride the falling-stone animation
        stoneToggleButton = makeDeckToggle("", 240);
        stoneToggleButton.addActionListener(e ->
        {
            stoneAnimationsEnabled = !stoneAnimationsEnabled;
            refreshStoneToggle();
        });

        // the commander's tale: lore for whichever commander was last chosen
        commanderLoreLabel = new JLabel();
        commanderLoreLabel.setFont(new Font("Serif", Font.ITALIC, 14));
        commanderLoreLabel.setForeground(INK);
        commanderLoreLabel.setBackground(PARCHMENT);
        commanderLoreLabel.setOpaque(true);
        commanderLoreLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WOOD_EDGE, 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        britonsSelector.addActionListener(e ->
        {
            refreshCommanderLore((PlayerType) britonsSelector.getSelectedItem());
            refreshNameRows();
        });
        franksSelector.addActionListener(e ->
        {
            refreshCommanderLore((PlayerType) franksSelector.getSelectedItem());
            refreshNameRows();
        });

        // the name field rides the same row as its side's selector
        JPanel britonsRow = makeFieldRow("Britons (Player 1):", britonsSelector);
        britonsRow.add(britonsNameField);
        JPanel franksRow = makeFieldRow("Franks (Player 2):", franksSelector);
        franksRow.add(franksNameField);

        gblAdd(deck, britonsRow, 0, STD_P);
        gblAdd(deck, franksRow, 1, STD_P);
        gblAdd(deck, modeRow, 2, STD_P);
        gblAdd(deck, stoneToggleButton, 3, STD_P);
        gblAdd(deck, commanderLoreLabel, 4, STD_P);

        refreshModeToggle();
        refreshStoneToggle();
        refreshNameRows();
        refreshCommanderLore((PlayerType) franksSelector.getSelectedItem());

        JButton beginButton = createMenuButton("Begin Battle", Color.WHITE, SEA_BLUE);
        JButton backButton = createMenuButton("Back", INK, PARCHMENT);
        beginButton.addActionListener(e -> launchBattle());
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "TitleScreen"));

        gblAdd(panel, headerLabel, 0, HEADER_P);
        gblAdd(panel, deck, 1, STD_P);
        gblAdd(panel, beginButton, 2, STD_P);
        gblAdd(panel, backButton, 3, BACK_P);

        return panel;
    }

    /**
     * Builds a deck-furniture toggle button: a carved plank that lights up
     * gold when its option is in force. Selection styling is applied by the
     * refresh methods, not here.
     *
     * @param text  the initial button text
     * @param width the preferred width in pixels
     * @return the styled toggle button
     */
    private JButton makeDeckToggle(String text, int width)
    {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, 36));
        button.setFont(new Font("Serif", Font.BOLD, 16));
        button.setFocusable(false);
        button.setBorder(BorderFactory.createLineBorder(WOOD_EDGE, 2));
        return button;
    }

    /**
     * Builds a label styled for the wooden deck: parchment text, no plate.
     *
     * @param text the label text
     * @return the styled label
     */
    private JLabel makeDeckLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Serif", Font.BOLD, 18));
        label.setForeground(PARCHMENT);
        return label;
    }

    /**
     * Builds a name field styled as deck furniture: parchment page, ink
     * script, tarred rim.
     *
     * @param defaultName the prefilled name
     * @return the styled text field
     */
    private javax.swing.JTextField makeNameField(String defaultName)
    {
        javax.swing.JTextField field = new javax.swing.JTextField(defaultName, 10);
        field.setFont(new Font("Serif", Font.BOLD, 10));
        field.setForeground(INK);
        field.setBackground(PARCHMENT);
        field.setCaretColor(INK);

        // the border itself supplies the honorific: the title reads
        // Commander above whatever name is signed inside it
        javax.swing.border.TitledBorder titled = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(WOOD_EDGE, 2), "Commander");
        titled.setTitleColor(WOOD_EDGE);
        titled.setTitleFont(new Font("Serif", Font.BOLD, 8));

        field.setBorder(BorderFactory.createCompoundBorder(titled,
            BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        return field;
    }

    /**
     * Shows each side's muster-roll field only while a human commands it; AI
     * commanders already have names the harbour-folk gave them.
     */
    private void refreshNameRows()
    {
        britonsNameField.setVisible(britonsSelector.getSelectedItem() == PlayerType.HUMAN);
        franksNameField.setVisible(franksSelector.getSelectedItem() == PlayerType.HUMAN);
    }

    /**
     * Reads a name field, returning the trimmed name or null when blank.
     *
     * @param field the field to read
     * @return the trimmed name, or null
     */
    private String readName(javax.swing.JTextField field)
    {
        String text = field.getText() == null ? "" : field.getText().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Restyles the rules toggle so the chosen plank glows gold and the
     * other recedes into the tar.
     */
    private void refreshModeToggle()
    {
        styleToggle(classicModeButton, !salvoSelected);
        styleToggle(salvoModeButton, salvoSelected);
    }

    /**
     * Renames and restyles the stone-volley toggle to show its state.
     */
    private void refreshStoneToggle()
    {
        stoneToggleButton.setText(
            "Stone Volleys: " + (stoneAnimationsEnabled ? "SHOWN" : "INSTANT"));
        styleToggle(stoneToggleButton, stoneAnimationsEnabled);
    }

    /**
     * Applies selected/unselected deck-toggle colors to a button.
     *
     * @param button   the toggle to restyle
     * @param selected whether its option is in force
     */
    private void styleToggle(JButton button, boolean selected)
    {
        button.setBackground(selected ? GOLD : WOOD_EDGE);
        button.setForeground(selected ? INK : PARCHMENT);
    }

    /**
     * Shows the tavern-tale for the commander most recently chosen on either
     * side: their nickname in bold, then the description.
     *
     * @param type the commander type to describe
     */
    private void refreshCommanderLore(PlayerType type)
    {
        if (type == null)
            return;
        commanderLoreLabel.setText("<html><div style='width:360px'><tt><b>"
            + "<span style='color: #dd0000;'>Captain's Log: </span></b><i>"
            + "<span style='font-weight: 900; color: #332421;'>"
            + type.getNickname() + "</span></i><br>" + type.getLore()
            + "</tt></div></html>");
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
        // dress the stock combo as deck furniture: parchment face, tarred rim
        box.setBackground(PARCHMENT);
        box.setForeground(INK);
        box.setBorder(BorderFactory.createLineBorder(WOOD_EDGE, 2));
        box.setFocusable(false);
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
        row.add(makeDeckLabel(labelText));
        row.add(control);
        return row;
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

        // every human commander must sign the muster roll -- any name will
        // do, so long as there IS one
        String britonsName = readName(britonsNameField);
        String franksName = readName(franksNameField);
        if ((britons == PlayerType.HUMAN && britonsName == null)
         || (franks == PlayerType.HUMAN && franksName == null))
        {
            javax.swing.JOptionPane.showMessageDialog(mainFrame,
                "Every mortal commander must sign the muster roll -- "
                + "a name is required.",
                "Unsigned muster roll",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (Component comp : mainPanel.getComponents())
            if (comp instanceof GameController)
                mainPanel.remove(comp);

        MatchConfig config = new MatchConfig(britons, franks,
            britonsName, franksName, salvoSelected, stoneAnimationsEnabled);

        GameController controller = controllerFactory.createController(
            config, () -> cardLayout.show(mainPanel, "TitleScreen"));

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
