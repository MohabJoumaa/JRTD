package monopoly;

import java.awt.Color;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioSystem;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import jiconfont.icons.font_awesome.FontAwesome;

public final class GameGraphics extends javax.swing.JFrame implements PlayersScanning {

    private volatile List<String> ipAddresses;
    private Server server = null;
    private Timer scanningPlayersTimer = null;
    private volatile Map<String, Boolean> playersQueue;
    private volatile Map<String, Double> playersPoints;
    private volatile Map<String, String> playersNicknames;
    private final String hostIPAddress;
    private String data, winner;
    private final String nickname;
    private volatile int playerOrder = 0, firstDicePoints = 0, secondDicePoints = 0;
    private final Icon eercast;
    private final SoundEffects soundeffects;
    private final File rollingDiceSoundEffectFile = new File("Sounds/RollingDiceSoundEffect.wav"),
            diceFace1 = new File("Images/1.jpg"),
            diceFace2 = new File("Images/2.jpg"),
            diceFace3 = new File("Images/3.jpg"),
            diceFace4 = new File("Images/4.jpg"),
            diceFace5 = new File("Images/5.jpg"),
            diceFace6 = new File("Images/6.jpg"),
            rollingDiceSoundEffect;
    private ImageIcon diceFace1Image, diceFace2Image, diceFace3Image, diceFace4Image, diceFace5Image, diceFace6Image;
    private volatile List<ImageIcon> diceFacesList;
    private boolean stopGame = false, playersToDBOneTime = true;
    private final SQLiteDatabase db;
    private Thread updatePointsToDBThread = null;
    private SwingWorker startUpdatePointsToDBThread = null;

    public GameGraphics(String hostIPAddress, Server server,
            List<String> ipAddresses, SQLiteDatabase db, String nickname) {
        IconFontSwing.register(FontAwesome.getIconFont());
        this.eercast = IconFontSwing.buildIcon(FontAwesome.EERCAST, 12, Color.WHITE);
        this.nickname = nickname;
        this.db = db;
        this.hostIPAddress = hostIPAddress;
        this.data = this.hostIPAddress + ":true";
        this.server = server;
        this.ipAddresses = ipAddresses;
        this.playersQueue = new HashMap<>(this.ipAddresses.size());
        this.playersPoints = new HashMap<>(this.ipAddresses.size());
        this.playersNicknames = new HashMap<>(this.ipAddresses.size());
        this.playersNicknames.put(this.hostIPAddress, this.nickname);
        this.rollingDiceSoundEffect = new File(this.rollingDiceSoundEffectFile.getAbsolutePath());
        this.diceFace1Image = new ImageIcon(this.diceFace1.getAbsolutePath());
        this.diceFace2Image = new ImageIcon(this.diceFace2.getAbsolutePath());
        this.diceFace3Image = new ImageIcon(this.diceFace3.getAbsolutePath());
        this.diceFace4Image = new ImageIcon(this.diceFace4.getAbsolutePath());
        this.diceFace5Image = new ImageIcon(this.diceFace5.getAbsolutePath());
        this.diceFace6Image = new ImageIcon(this.diceFace6.getAbsolutePath());
        this.soundeffects = new SoundEffects();
        this.sendPlayersQueue();
        this.initComponents();
        this.setIconImage(new ImageIcon(new File("Images/icon.png").getAbsolutePath()).getImage());
        this.initDicesIcons();
        this.setPlayersInPanel();
        this.addPlayersToQueue();
    }

    protected void runTimer() {
        this.scanningPlayersTimer = new Timer();
        this.runScanningPlayersTask();
    }

    private void addPlayersToQueue() {
        this.db.insertGameRecord();
        //System.out.println(this.ipAddresses.size());
        //System.out.println(this.ipAddresses.toString());
        //System.out.println(this.playersNicknames.toString());
        this.ipAddresses.stream().forEach((String ipAddresse) -> {
            GameGraphics.this.playersQueue.put(ipAddresse, false);
            GameGraphics.this.playersPoints.put(ipAddresse, 1500.0d);
        });
        this.setPlayersOrder();
    }

    private void setPlayersInPanel() {
        int i = 5;
        this.jPanel.removeAll();
        //System.out.println(this.playersPoints.toString());
        for (String ip : this.playersPoints.keySet()) {
            String playerViewer = (this.nickname.isEmpty()) ? ip : this.playersNicknames.get(ip);
            javax.swing.JLabel playerLabel = new javax.swing.JLabel(playerViewer + ": "
                    + String.valueOf((double) this.playersPoints.get(ip)) + "M");
            Font font = new Font("Serif", Font.BOLD, 13);
            playerLabel.setFont(font);
            playerLabel.setIcon(this.eercast);
            playerLabel.setBounds(10, i, 190, 40);
            this.jPanel.add(playerLabel);
            i += 25;
        }
        this.jPanel.revalidate();
        this.jPanel.repaint();
    }

    private void initDicesIcons() {
        this.diceFacesList = new ArrayList<>(6);
        Image img1 = this.diceFace1Image.getImage().getScaledInstance(70, 60, Image.SCALE_SMOOTH);
        Image img2 = this.diceFace2Image.getImage().getScaledInstance(70, 60, Image.SCALE_SMOOTH);
        Image img3 = this.diceFace3Image.getImage().getScaledInstance(70, 60, Image.SCALE_SMOOTH);
        Image img4 = this.diceFace4Image.getImage().getScaledInstance(70, 60, Image.SCALE_SMOOTH);
        Image img5 = this.diceFace5Image.getImage().getScaledInstance(70, 60, Image.SCALE_SMOOTH);
        Image img6 = this.diceFace6Image.getImage().getScaledInstance(70, 60, Image.SCALE_SMOOTH);
        this.diceFace1Image = new ImageIcon(img1);
        this.diceFace2Image = new ImageIcon(img2);
        this.diceFace3Image = new ImageIcon(img3);
        this.diceFace4Image = new ImageIcon(img4);
        this.diceFace5Image = new ImageIcon(img5);
        this.diceFace6Image = new ImageIcon(img6);
        this.diceFacesList.add(this.diceFace1Image);
        this.diceFacesList.add(this.diceFace2Image);
        this.diceFacesList.add(this.diceFace3Image);
        this.diceFacesList.add(this.diceFace4Image);
        this.diceFacesList.add(this.diceFace5Image);
        this.diceFacesList.add(this.diceFace6Image);
        this.dice1.setIcon(this.diceFace1Image);
        this.dice2.setIcon(this.diceFace1Image);
    }

    private void setPlayersOrder() {
        //System.out.println(this.playersQueue);
        if (this.playersToDBOneTime) {
            this.playersQueue.replace(this.ipAddresses.get(this.playerOrder++), true);
        } else {
            this.playersQueue.replace(this.ipAddresses.get(this.playerOrder), true);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        playersStatusesLabel = new javax.swing.JLabel();
        go = new javax.swing.JButton();
        jPanel = new javax.swing.JPanel();
        dice2 = new javax.swing.JLabel();
        dice1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Just Roll The Dice");
        setResizable(false);

        playersStatusesLabel.setBackground(new java.awt.Color(0, 0, 0));
        playersStatusesLabel.setFont(new java.awt.Font("Times New Roman", 0, 14)); // NOI18N
        playersStatusesLabel.setForeground(new java.awt.Color(255, 255, 255));
        playersStatusesLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        playersStatusesLabel.setToolTipText("");
        playersStatusesLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        playersStatusesLabel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 0, 0), 3, true));
        playersStatusesLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        playersStatusesLabel.setOpaque(true);

        go.setBackground(new java.awt.Color(205, 255, 220));
        go.setFont(new java.awt.Font("Times New Roman", 0, 24)); // NOI18N
        go.setForeground(new java.awt.Color(255, 0, 0));
        go.setText("Go");
        go.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        go.setCursor(new java.awt.Cursor(java.awt.Cursor.CROSSHAIR_CURSOR));
        go.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goActionPerformed(evt);
            }
        });

        jPanel.setBackground(new java.awt.Color(230, 150, 100));
        jPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 2, true));
        jPanel.setFont(new java.awt.Font("Times New Roman", 0, 11)); // NOI18N

        javax.swing.GroupLayout jPanelLayout = new javax.swing.GroupLayout(jPanel);
        jPanel.setLayout(jPanelLayout);
        jPanelLayout.setHorizontalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 228, Short.MAX_VALUE)
        );
        jPanelLayout.setVerticalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 190, Short.MAX_VALUE)
        );

        dice2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 2, true));

        dice1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 2, true));
        dice1.setOpaque(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(playersStatusesLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(go, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(232, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(232, 232, 232))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(dice2, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(dice1, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(116, 116, 116))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(playersStatusesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 132, Short.MAX_VALUE)
                .addComponent(jPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dice2, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dice1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addComponent(go, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void goActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goActionPerformed
        this.playersQueue.replace(this.hostIPAddress, false);
        this.diceRolling();
        this.setPlayersInPanel();
        this.playerOrder++;
        this.setPlayersOrder();
        this.sendPlayersQueue();
    }//GEN-LAST:event_goActionPerformed

    private void diceRolling() {
        Random randomDiceRolling = new Random();
        int m, dice1Face = randomDiceRolling.nextInt(6), dice2Face = randomDiceRolling.nextInt(6);;
        this.soeffects(this.rollingDiceSoundEffect);
        try {
            Thread.sleep(500L);
        } catch (InterruptedException ex) {
            Logger.getLogger(GameGraphics.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getMessage());
        }
        this.dice1.setIcon(this.diceFacesList.get(dice1Face));
        this.dice2.setIcon(this.diceFacesList.get(dice2Face));
        if (dice1Face == 0 && dice2Face == 0) {
            this.firstDicePoints = 25;
            this.secondDicePoints = 25;
        } else if (dice1Face == 0 && dice2Face == 1) {
            this.firstDicePoints = 35;
            this.secondDicePoints = 65;
        } else if (dice1Face == 0 && dice2Face == 2) {
            this.firstDicePoints = 50;
            this.secondDicePoints = 100;
        } else if (dice1Face == 0 && dice2Face == 3) {
            this.firstDicePoints = 70;
            this.secondDicePoints = 130;
        } else if (dice1Face == 0 && dice2Face == 4) {
            this.firstDicePoints = 150;
            this.secondDicePoints = 100;
        } else if (dice1Face == 0 && dice2Face == 5) {
            this.firstDicePoints = 170;
            this.secondDicePoints = 130;
        } else if (dice1Face == 1 && dice2Face == 0) {
            this.firstDicePoints = 165;
            this.secondDicePoints = 185;
        } else if (dice1Face == 1 && dice2Face == 1) {
            this.firstDicePoints = 200;
            this.secondDicePoints = 200;
        } else if (dice1Face == 1 && dice2Face == 2) {
            this.firstDicePoints = 238;
            this.secondDicePoints = 212;
        } else if (dice1Face == 1 && dice2Face == 3) {
            this.firstDicePoints = 384;
            this.secondDicePoints = 116;
        } else if (dice1Face == 1 && dice2Face == 4) {
            this.firstDicePoints = 110;
            this.secondDicePoints = 440;
        } else if (dice1Face == 1 && dice2Face == 5) {
            this.firstDicePoints = 150;
            this.secondDicePoints = 450;
        } else if (dice1Face == 2 && dice2Face == 0) {
            this.firstDicePoints = 532;
            this.secondDicePoints = 118;
        } else if (dice1Face == 2 && dice2Face == 1) {
            this.firstDicePoints = 398;
            this.secondDicePoints = 302;
        } else if (dice1Face == 2 && dice2Face == 2) {
            this.firstDicePoints = 81;
            this.secondDicePoints = 669;
        } else if (dice1Face == 2 && dice2Face == 3) {
            this.firstDicePoints = 600;
            this.secondDicePoints = 200;
        } else if (dice1Face == 2 && dice2Face == 4) {
            this.firstDicePoints = 53;
            this.secondDicePoints = 797;
        } else if (dice1Face == 2 && dice2Face == 5) {
            this.firstDicePoints = 700;
            this.secondDicePoints = 200;
        } else if (dice1Face == 3 && dice2Face == 0) {
            this.firstDicePoints = 800;
            this.secondDicePoints = 150;
        } else if (dice1Face == 3 && dice2Face == 1) {
            this.firstDicePoints = 500;
            this.secondDicePoints = 500;
        } else if (dice1Face == 3 && dice2Face == 2) {
            this.firstDicePoints = 486;
            this.secondDicePoints = 564;
        } else if (dice1Face == 3 && dice2Face == 3) {
            this.firstDicePoints = 970;
            this.secondDicePoints = 130;
        } else if (dice1Face == 3 && dice2Face == 4) {
            this.firstDicePoints = 150;
            this.secondDicePoints = 1000;
        } else if (dice1Face == 3 && dice2Face == 5) {
            this.firstDicePoints = 500;
            this.secondDicePoints = 700;
        } else if (dice1Face == 4 && dice2Face == 0) {
            this.firstDicePoints = 500;
            this.secondDicePoints = 750;
        } else if (dice1Face == 4 && dice2Face == 1) {
            this.firstDicePoints = 600;
            this.secondDicePoints = 700;
        } else if (dice1Face == 4 && dice2Face == 2) {
            this.firstDicePoints = 893;
            this.secondDicePoints = 457;
        } else if (dice1Face == 4 && dice2Face == 3) {
            this.firstDicePoints = 189;
            this.secondDicePoints = 1211;
        } else if (dice1Face == 4 && dice2Face == 4) {
            this.firstDicePoints = 1000;
            this.secondDicePoints = 450;
        } else if (dice1Face == 4 && dice2Face == 5) {
            this.firstDicePoints = 700;
            this.secondDicePoints = 800;
        } else if (dice1Face == 5 && dice2Face == 0) {
            this.firstDicePoints = 1000;
            this.secondDicePoints = 550;
        } else if (dice1Face == 5 && dice2Face == 1) {
            this.firstDicePoints = 1300;
            this.secondDicePoints = 300;
        } else if (dice1Face == 5 && dice2Face == 2) {
            this.firstDicePoints = 483;
            this.secondDicePoints = 1167;
        } else if (dice1Face == 5 && dice2Face == 3) {
            this.firstDicePoints = 500;
            this.secondDicePoints = 1200;
        } else if (dice1Face == 5 && dice2Face == 4) {
            this.firstDicePoints = 847;
            this.secondDicePoints = 903;
        } else if (dice1Face == 5 && dice2Face == 5) {
            this.firstDicePoints = 900;
            this.secondDicePoints = 900;
        }
        m = this.firstDicePoints + this.secondDicePoints;
        String address = this.hostIPAddress;
        double points = this.playersPoints.get(this.hostIPAddress) + m;
        this.playersPoints.replace(address, points);
        this.startUpdatePoints(address, (int) points);
        this.startUpdateDicesPoints(address, this.firstDicePoints, this.secondDicePoints);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel dice1;
    private javax.swing.JLabel dice2;
    private javax.swing.JButton go;
    private javax.swing.JPanel jPanel;
    protected javax.swing.JLabel playersStatusesLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public TimerTask scanningPlayersTask() {
        return new TimerTask() {
            @Override
            public void run() {
                GameGraphics.this.scanForPlayers();
                GameGraphics.this.setGoButton();
                GameGraphics.this.checkQueue();
                GameGraphics.this.checkWiner();
            }
        };
    }

    private synchronized void checkWiner() {
        this.playersPoints.keySet().stream().filter((player) -> (this.playersPoints.get(player) >= 10000d)).forEachOrdered((String player) -> {
            GameGraphics.this.winner = player;
            GameGraphics.this.stopGame = true;
            GameGraphics.this.sendPlayersQueue();
            GameGraphics.this.db.closeConnection();
            System.exit(0);
        });
    }

    @Override
    public synchronized void scanForPlayers() {
        this.playersStatusesLabel.setText("");
        ArrayList<String> playersToRemove = new ArrayList<>(this.ipAddresses.size());
        List<String> copy = new ArrayList<>(this.ipAddresses);
        copy.forEach((playerIpAddress) -> {
            String playerViewer = (this.nickname.isEmpty()) ? playerIpAddress : this.playersNicknames.get(playerIpAddress);
            try {
                if (InetAddress.getByName(playerIpAddress).isReachable(10000)) {
                    this.playersStatusesLabel.setText(this.playersStatusesLabel.getText()
                            + playerViewer + " Connected   ");
                } else {
                    this.playersStatusesLabel.setText(this.playersStatusesLabel.getText()
                            + playerViewer + " Not Connected   ");
                    this.db.deletePlayerRecord(playerIpAddress);
                    playersToRemove.add(playerIpAddress);
                }
                if (copy.indexOf(playerIpAddress) == 4) {
                    this.playersStatusesLabel.setText(this.playersStatusesLabel.getText() + "\n");
                }
            } catch (IOException ex) {
                Logger.getLogger(GameGraphics.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println(ex.getMessage());
            }
        });
        playersToRemove.trimToSize();
    }

    @Override
    public void scanForPlayers(String part) {
    }

    @Override
    public TimerTask scanningPlayersTask(String part) {
        return null;
    }

    private void runScanningPlayersTask() {
        TimerTask scanningPlayersTask = this.scanningPlayersTask();
        this.scanningPlayersTimer.scheduleAtFixedRate(scanningPlayersTask, 50L, 50L);
    }

    private void sendPlayersQueue() {
        try {
            Data data = new Data(this.playersQueue, this.playersPoints, this.playersNicknames, this.stopGame, this.winner);
            this.server.sendDataToClient(this.data.split(":")[0], data);
            if (data.getGameStopFlag()) {
                JOptionPane.showMessageDialog(GameGraphics.this, "Player " + data.getWinner() + " Won The Game");
            }
        } catch (HeadlessException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void checkQueue() {
        boolean isQueueFinished = (this.playerOrder == this.playersQueue.size());
        if (isQueueFinished) {
            this.playerOrder = 0;
            //System.out.println("Queue Has Been Finished");
            this.ipAddresses.forEach((ip) -> {
                this.playersQueue.replace(ip, false);
            });
            this.setPlayersOrder();
        }
    }

    private void setGoButton() {
        if (this.playersQueue.get(this.hostIPAddress)) {
            this.go.setEnabled(true);
        } else {
            this.go.setEnabled(false);
        }
    }

    private void updatePlayersPointsInDB() {
        this.playersPoints.entrySet().forEach((entry) -> {
            this.startUpdatePoints(entry.getKey(), entry.getValue().intValue());
        });
    }

    private void extractDicesPointsAndUpdateDB() {
        this.playersPoints.entrySet().forEach((entry) -> {
            switch (entry.getValue().intValue()) {
                case 50:
                    this.firstDicePoints = 25;
                    this.secondDicePoints = 25;
                    break;
                case 100:
                    this.firstDicePoints = 35;
                    this.secondDicePoints = 65;
                    break;
                case 150:
                    this.firstDicePoints = 50;
                    this.secondDicePoints = 100;
                    break;
                case 200:
                    this.firstDicePoints = 70;
                    this.secondDicePoints = 130;
                    break;
                case 250:
                    this.firstDicePoints = 150;
                    this.secondDicePoints = 100;
                    break;
                case 300:
                    this.firstDicePoints = 170;
                    this.secondDicePoints = 130;
                    break;
                case 350:
                    this.firstDicePoints = 165;
                    this.secondDicePoints = 185;
                    break;
                case 400:
                    this.firstDicePoints = 200;
                    this.secondDicePoints = 200;
                    break;
                case 450:
                    this.firstDicePoints = 238;
                    this.secondDicePoints = 212;
                    break;
                case 500:
                    this.firstDicePoints = 384;
                    this.secondDicePoints = 116;
                    break;
                case 550:
                    this.firstDicePoints = 110;
                    this.secondDicePoints = 440;
                    break;
                case 600:
                    this.firstDicePoints = 150;
                    this.secondDicePoints = 450;
                    break;
                case 650:
                    this.firstDicePoints = 532;
                    this.secondDicePoints = 118;
                    break;
                case 700:
                    this.firstDicePoints = 398;
                    this.secondDicePoints = 302;
                    break;
                case 750:
                    this.firstDicePoints = 81;
                    this.secondDicePoints = 669;
                    break;
                case 800:
                    this.firstDicePoints = 600;
                    this.secondDicePoints = 200;
                    break;
                case 850:
                    this.firstDicePoints = 53;
                    this.secondDicePoints = 797;
                    break;
                case 900:
                    this.firstDicePoints = 700;
                    this.secondDicePoints = 200;
                    break;
                case 950:
                    this.firstDicePoints = 800;
                    this.secondDicePoints = 150;
                    break;
                case 1000:
                    this.firstDicePoints = 500;
                    this.secondDicePoints = 500;
                    break;
                case 1050:
                    this.firstDicePoints = 486;
                    this.secondDicePoints = 564;
                    break;
                case 1100:
                    this.firstDicePoints = 970;
                    this.secondDicePoints = 130;
                    break;
                case 1150:
                    this.firstDicePoints = 150;
                    this.secondDicePoints = 1000;
                    break;
                case 1200:
                    this.firstDicePoints = 500;
                    this.secondDicePoints = 700;
                    break;
                case 1250:
                    this.firstDicePoints = 500;
                    this.secondDicePoints = 750;
                    break;
                case 1300:
                    this.firstDicePoints = 600;
                    this.secondDicePoints = 700;
                    break;
                case 1350:
                    this.firstDicePoints = 893;
                    this.secondDicePoints = 457;
                    break;
                case 1400:
                    this.firstDicePoints = 189;
                    this.secondDicePoints = 1211;
                    break;
                case 1450:
                    this.firstDicePoints = 1000;
                    this.secondDicePoints = 450;
                    break;
                case 1500:
                    this.firstDicePoints = 700;
                    this.secondDicePoints = 800;
                    break;
                case 1550:
                    this.firstDicePoints = 1000;
                    this.secondDicePoints = 550;
                    break;
                case 1600:
                    this.firstDicePoints = 1300;
                    this.secondDicePoints = 300;
                    break;
                case 1650:
                    this.firstDicePoints = 483;
                    this.secondDicePoints = 1167;
                    break;
                case 1700:
                    this.firstDicePoints = 500;
                    this.secondDicePoints = 1200;
                    break;
                case 1750:
                    this.firstDicePoints = 847;
                    this.secondDicePoints = 903;
                    break;
                case 1800:
                    this.firstDicePoints = 900;
                    this.secondDicePoints = 900;
                    break;
            }
            this.startUpdateDicesPoints(entry.getKey(), this.firstDicePoints, this.secondDicePoints);
        });
    }

    protected void setData(String data, Data d) {
        this.data = data;
        this.playersQueue.putAll(d.getPlayersQueue());
        this.playersPoints.putAll(d.getPlayersPoints());
        this.playersNicknames.putAll(d.getPlayersNicknames());
        if (this.playersToDBOneTime) {
            this.ipAddresses.stream().forEach((String ipAddresse) -> {
                this.db.insertPlayerRecord(this.playersNicknames.get(ipAddresse), ipAddresse);
                this.db.insertPlayingRecord(ipAddresse);
            });
            this.playersToDBOneTime = false;
        }
        this.updatePlayersPointsInDB();
        this.extractDicesPointsAndUpdateDB();
        this.setPlayersInPanel();
        this.playerOrder++;
        this.checkQueue();
        this.setPlayersOrder();
        //System.out.println(this.playersQueue);
        //System.out.println(this.playersPoints);
    }

    protected void setServer(Server server) {
        this.server = server;
    }

    private synchronized void soeffects(File file) {
        try {
            this.soundeffects.clip = AudioSystem.getClip();
            this.soundeffects.clip.open(AudioSystem.getAudioInputStream(file));
            this.soundeffects.clip.start();
        } catch (IOException | javax.sound.sampled.LineUnavailableException | javax.sound.sampled.UnsupportedAudioFileException iOException) {
        }
    }

    private void startUpdatePoints(String ip, int result) {
        this.startUpdatePointsToDBThread = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                GameGraphics.this.updatePointsToDBThread = new Thread(() -> {
                    GameGraphics.this.db.updatePlayerPoints(ip, result);
                });
                GameGraphics.this.updatePointsToDBThread.start();
                try {
                    GameGraphics.this.updatePointsToDBThread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(GameGraphics.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println(ex.getMessage());
                }
                return null;
            }
        };
        this.startUpdatePointsToDBThread.execute();
    }

    private void startUpdateDicesPoints(String ip, int dice1, int dice2) {
        this.startUpdatePointsToDBThread = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                GameGraphics.this.updatePointsToDBThread = new Thread(() -> {
                    GameGraphics.this.db.updateRollingDicesPoints(dice1, dice2, ip);
                });
                GameGraphics.this.updatePointsToDBThread.start();
                try {
                    GameGraphics.this.updatePointsToDBThread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(GameGraphics.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println(ex.getMessage());
                }
                return null;
            }
        };
        this.startUpdatePointsToDBThread.execute();
    }

}
