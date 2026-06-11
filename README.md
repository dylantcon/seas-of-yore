# seas-of-yore
A medieval 'Battleship' inspired board game, with new features and game modes. Custom-drawn sprites and an engaging user interface enhance the core gameplay features.

## Overview (May Describe Some Stretch Goals for v1.0)
Seas of Yore is a Java-based battleship-inspired game, which honors the original rules of Battleship, while also building on the original features of the game. This project is complete with medieval style sprites, UI, a thematic soundtrack, and interactive sound effects as the game is played. It can be played singleplayer against four different AI difficulties, or as a multiplayer game, with a human player versus a human player. There also will be save-game functionality, so if you want to come back to a game later, you absolutely can. This game is built entirely in Java, and relies only on core JDK features and a few image sprites and sound files. These are all packed into the `SeasOfYore.jar` file that is enclosed in this repository. So, my hope is that if you have the JRE installed, you should be able to just double-click `SeasOfYore.jar` and it will boot up.

### Images
#### Title Screen
![title](https://javalab.dconn.dev/apps/seas-of-yore/thumbnail.png)
#### Local Setup
![singleplayer](https://drive.google.com/file/d/1qrcH9WTCUXnkOpc0s_H0iPcunVoKeSCS/view?usp=drive_link)
#### Online Setup
![multiplayer](https://drive.google.com/file/d/1Qzpp5cVydsl1Vgj8LAAkouK2w9NCC4db/view?usp=drive_link)
#### In-Game
![battle](https://drive.google.com/file/d/1J4gGdo1Nu7ZtW9UInjv0hA-QyaKl8r4J/view?usp=drive_link)

## Current Features
**Intelligent Computer Players**: Each of the AI players will select locations on the board based on a heat-map matrix, that is a reference to the opposing player's board, sans their ships. Easier difficulties will deviate slightly from the heatmap's suggestions, and harder ones will rely on it entirely. When an AI player takes a turn, the shot fired will contribute to a floating point confidence factor, with misses decreasing confidence in the surrounding area, and hits increasing it. The confidence factor increments surrounding cells in the northward, eastward, westward, and southward directions, extending relative to the ceiling of the average length of the enemy's remaining ships. Additionally, if a shot is taken near the edge resulting in a hit, and the projection of the confidence factor would extend outside of the board, the valid cells will receive additional increments to confidence relative to the number of projected 'confidence cells' that transcended the limits of the board. The heat-map will feature a decay-factor, with older information losing relevance as the game progresses. If there is insufficient data on the heat-map, the AI players will default to a safe pattern of checking cells until there IS sufficient heat-map data.

**Interactive User Experience**: The entire game will have interactive sound effects upon firing a shot, hitting or missing a ship, and sinking a ship. Additionally, the opposing teams are to be the historical enemies the Britons and the Franks, each sporting their quintessential flags, being the Guilded 3 Lions, and the Fleur De Lis. All user input is to be straightforward and intuitive, with selection of enemy board cells being through mouse input. The goal is to construct an immersive and fun user interface that doesn't just facilitate the gameplay, but adds to it. The ships have been renamed from the `Destroyer (2)`, the `Submarine (3)`, the `Cruiser (3)`, the `Battleship (4)`, and the `Carrier (5)`, to aptly chosen medieval vessels, being the `Crayer (2)`, the `Hoy (3)`, the `Galley (3)`, the `Cog (4)`, and the `Galleon (5)`.

**SALVO Mode**: Supports the SALVO variation of Battleship, where a player must fire a number of shots proportional to their number of ships at the start of their turn. Higher stakes, more opportunities to steamroll opponents.

**Graphical Interface**: The GUI for this game is constructed entirely using the core functionality of the JDK, as to facilitate the highest degree of system portability possible. The game will automatically rescale to fit within the current window-frame, using relative component rescaling.

**Networked Multiplayer**: Supports LAN games between two Seas of Yore clients and online games via a simple relay server hosted specifically for this app. Can host and join via room codes or directly via IPv4 address and port. Both Classic and SALVO modes work.

## Planned Features
**New Optional Gameplay Mode**: A new added gameplay mode, `Royal Skirmish`, which users may select upon opening the initial game menu. It adds some fun and exciting new features that aim to spice up the core gameplay, while also staying true to the foundation of the original game itself. See the devoted section in this README to learn more about it.

### Royal Skirmish
**Description**: In this new mode, during gameplay, each player accrues `Gold`, either at the end of their turn, or upon successfully hitting a ship. `Gold` may be spent on `Special Ammunition`, each of which has a unique effect on the opposing player's board. There are some examples of potential special ammunition types in the subsection below.

#### Royal Skirmish Abilities

**Flare**: "Illuminate the yonder seas, in a three by three area! If thy enemy hast placed portions of their warships in the region thou chooses, thine flair shalt glow a green colour. (3 GOLD)"

**Onager**: "Thy catapult is temporarily upgraded, and fires three stones instead of one! Thou may choose to rain them down on thine enemy either vertically, horizontally, or diagonally. (7 GOLD)"

**Ballista Bolt**: "Lo, a bolt forged of iron! Loosen upon thine enemy’s waters and strike a single square with deadly accuracy, earning **TRIPLE** the gold upon a successful hit. If thy target is missed, thou dost still earn 1 GOLD. (Cost: 4 GOLD)"

**Aegis Shield**: "Raise thy mighty shield, protecteth thine chosen ship for one turn. If thy foe strike this ship while shielded, their attack shalt be for naught, and they shan't learn of its true position! (Cost: 5 GOLD)"
