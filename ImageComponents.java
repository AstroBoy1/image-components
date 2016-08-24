

    /*

     * ImageComponents.java

     * A5 Solution by Michael Omori with UWNetID: 1329128.

     // need to finish computeConnectedComponents union part

     *

     * CSE 373, University of Washington, Winter 2016.

     *

     * Starter Code for CSE 373 Assignment 5, Part II.    Starter Code Version 1.

     * S. Tanimoto

     *

     */

     

    import java.util.Hashtable;

    import java.util.List;

    import java.util.Map;

    import java.awt.Dimension;

    import java.awt.Graphics;

    import java.awt.event.ActionEvent;

    import java.awt.event.ActionListener;

    import java.awt.event.WindowAdapter;

    import java.awt.event.WindowEvent;

    import java.awt.image.BufferedImage;

    import java.awt.image.BufferedImageOp;

    import java.awt.image.ByteLookupTable;

    import java.awt.image.ConvolveOp;

    import java.awt.image.Kernel;

    import java.awt.image.LookupOp;

    import java.io.File;

    import java.io.IOException;

    import java.util.ArrayList;

    import java.util.Collections;

    import java.util.Comparator;

    import java.util.HashMap;

    import java.util.Arrays;

    import javax.imageio.ImageIO;

    import javax.swing.JCheckBoxMenuItem;

    import javax.swing.JFileChooser;

    import javax.swing.JFrame;

    import javax.swing.JMenu;

    import javax.swing.JMenuBar;

    import javax.swing.JMenuItem;

    import javax.swing.JOptionPane;

    import javax.swing.JPanel;

    import javax.swing.JPopupMenu;

    import javax.swing.filechooser.FileNameExtensionFilter;

     

    //TO DO: check connected pixels are edges

    public class ImageComponents extends JFrame implements ActionListener {

        public static ImageComponents appInstance; // Used in main().

     

        String startingImage = "gettysburg-address-p1.png";

        BufferedImage biTemp, biWorking, biFiltered; // These hold arrays of pixels.

        Graphics gOrig, gWorking; // Used to access the drawImage method.

        int w; // width of the current image.

        int h; // height of the current image.

     

        int[][] parentID; // For your forest of up-trees.

        HashMap <Integer, ArrayList <Integer>> IDcoordinates; // map containing pixelIDs and co-ordinates

     

        JPanel viewPanel; // Where the image will be painted.

        JPopupMenu popup;

        JMenuBar menuBar;

        JMenu fileMenu, imageOpMenu, ccMenu, helpMenu;

        JMenuItem loadImageItem, saveAsItem, exitItem;

        JMenuItem lowPassItem, highPassItem, photoNegItem, RGBThreshItem;

     

        JMenuItem CCItem1;

        JMenuItem aboutItem, helpItem;

     

        JFileChooser fileChooser; // For loading and saving images.

     

        public class Color {

            int r, g, b;

     

            Color(int r, int g, int b) {

                this.r = r; this.g = g; this.b = b;

            }

     

            double euclideanDistance(Color c2) {

                // TODOne

                double distance = Math.pow(this.r - c2.r, 2) + Math.pow(this.g - c2.g,2) + Math.pow(this.b - c2.b, 2);

                return distance;

            }

        }

     

     

        // Some image manipulation data definitions that won't change...

        static LookupOp PHOTONEG_OP, RGBTHRESH_OP;

        static ConvolveOp LOWPASS_OP, HIGHPASS_OP;

     

        public static final float[] SHARPENING_KERNEL = { // sharpening filter kernel

            0.f, -1.f,  0.f,

           -1.f,  5.f, -1.f,

            0.f, -1.f,  0.f

        };

     

        public static final float[] BLURRING_KERNEL = {

            0.1f, 0.1f, 0.1f,    // low-pass filter kernel

            0.1f, 0.2f, 0.1f,

            0.1f, 0.1f, 0.1f

        };

     

        public ImageComponents() { // Constructor for the application.

            setTitle("Image Analyzer");

            addWindowListener(new WindowAdapter() { // Handle any window close-box clicks.

                public void windowClosing(WindowEvent e) {System.exit(0);}

            });

     

            // Create the panel for showing the current image, and override its

            // default paint method to call our paintPanel method to draw the image.

            viewPanel = new JPanel(){public void paint(Graphics g) { paintPanel(g);}};

            add("Center", viewPanel); // Put it smack dab in the middle of the JFrame.

     

            // Create standard menu bar

            menuBar = new JMenuBar();

            setJMenuBar(menuBar);

            fileMenu = new JMenu("File");

            imageOpMenu = new JMenu("Image Operations");

            ccMenu = new JMenu("Connected Components");

            helpMenu = new JMenu("Help");

            menuBar.add(fileMenu);

            menuBar.add(imageOpMenu);

            menuBar.add(ccMenu);

            menuBar.add(helpMenu);

     

            // Create the File menu's menu items.

            loadImageItem = new JMenuItem("Load image...");

            loadImageItem.addActionListener(this);

            fileMenu.add(loadImageItem);

            saveAsItem = new JMenuItem("Save as full-color PNG");

            saveAsItem.addActionListener(this);

            fileMenu.add(saveAsItem);

            exitItem = new JMenuItem("Quit");

            exitItem.addActionListener(this);

            fileMenu.add(exitItem);

     

            // Create the Image Operation menu items.

            lowPassItem = new JMenuItem("Convolve with blurring kernel");

            lowPassItem.addActionListener(this);

            imageOpMenu.add(lowPassItem);

            highPassItem = new JMenuItem("Convolve with sharpening kernel");

            highPassItem.addActionListener(this);

            imageOpMenu.add(highPassItem);

            photoNegItem = new JMenuItem("Photonegative");

            photoNegItem.addActionListener(this);

            imageOpMenu.add(photoNegItem);

            RGBThreshItem = new JMenuItem("RGB Thresholds at 128");

            RGBThreshItem.addActionListener(this);

            imageOpMenu.add(RGBThreshItem);

     

     

            // Create CC menu stuff.

            CCItem1 = new JMenuItem("Compute Connected Components and Recolor");

            CCItem1.addActionListener(this);

            ccMenu.add(CCItem1);

     

            // Create the Help menu's item.

            aboutItem = new JMenuItem("About");

            aboutItem.addActionListener(this);

            helpMenu.add(aboutItem);

            helpItem = new JMenuItem("Help");

            helpItem.addActionListener(this);

            helpMenu.add(helpItem);

     

            // Initialize the image operators, if this is the first call to the constructor:

            if (PHOTONEG_OP==null) {

                byte[] lut = new byte[256];

                for (int j=0; j<256; j++) {

                    lut[j] = (byte)(256-j);

                }

                ByteLookupTable blut = new ByteLookupTable(0, lut);

                PHOTONEG_OP = new LookupOp(blut, null);

            }

            if (RGBTHRESH_OP==null) {

                byte[] lut = new byte[256];

                for (int j=0; j<256; j++) {

                    lut[j] = (byte)(j < 128 ? 0: 200);

                }

                ByteLookupTable blut = new ByteLookupTable(0, lut);

                RGBTHRESH_OP = new LookupOp(blut, null);

            }

            if (LOWPASS_OP==null) {

                float[] data = BLURRING_KERNEL;

                LOWPASS_OP = new ConvolveOp(new Kernel(3, 3, data),

                                            ConvolveOp.EDGE_NO_OP,

                                            null);

            }

            if (HIGHPASS_OP==null) {

                float[] data = SHARPENING_KERNEL;

                HIGHPASS_OP = new ConvolveOp(new Kernel(3, 3, data),

                                            ConvolveOp.EDGE_NO_OP,

                                            null);

            }

            loadImage(startingImage); // Read in the pre-selected starting image.

            setVisible(true); // Display it.

        }

     

        /*

         * Given a path to a file on the file system, try to load in the file

         * as an image.  If that works, replace any current image by the new one.

         * Re-make the biFiltered buffered image, too, because its size probably

         * needs to be different to match that of the new image.

         */

        public void loadImage(String filename) {

            try {

                biTemp = ImageIO.read(new File(filename));

                w = biTemp.getWidth();

                h = biTemp.getHeight();

                viewPanel.setSize(w,h);

                biWorking = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                gWorking = biWorking.getGraphics();

                gWorking.drawImage(biTemp, 0, 0, null);

                biFiltered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                pack(); // Lay out the JFrame and set its size.

                repaint();

            } catch (IOException e) {

                System.out.println("Image could not be read: "+filename);

                System.exit(1);

            }

        }

     

        /* Menu handlers

         */

        void handleFileMenu(JMenuItem mi){

            System.out.println("A file menu item was selected.");

            if (mi==loadImageItem) {

                File loadFile = new File("image-to-load.png");

                if (fileChooser==null) {

                    fileChooser = new JFileChooser();

                    fileChooser.setSelectedFile(loadFile);

                    fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", new String[] { "JPG", "JPEG", "GIF", "PNG" }));

                }

                int rval = fileChooser.showOpenDialog(this);

                if (rval == JFileChooser.APPROVE_OPTION) {

                    loadFile = fileChooser.getSelectedFile();

                    loadImage(loadFile.getPath());

                }

            }

            if (mi==saveAsItem) {

                File saveFile = new File("savedimage.png");

                fileChooser = new JFileChooser();

                fileChooser.setSelectedFile(saveFile);

                int rval = fileChooser.showSaveDialog(this);

                if (rval == JFileChooser.APPROVE_OPTION) {

                    saveFile = fileChooser.getSelectedFile();

                    // Save the current image in PNG format, to a file.

                    try {

                        ImageIO.write(biWorking, "png", saveFile);

                    } catch (IOException ex) {

                        System.out.println("There was some problem saving the image.");

                    }

                }

            }

            if (mi==exitItem) { this.setVisible(false); System.exit(0); }

        }

     

        void handleEditMenu(JMenuItem mi){

            System.out.println("An edit menu item was selected.");

        }

     

        void handleImageOpMenu(JMenuItem mi){

            System.out.println("An imageOp menu item was selected.");

            if (mi==lowPassItem) { applyOp(LOWPASS_OP); }

            else if (mi==highPassItem) { applyOp(HIGHPASS_OP); }

            else if (mi==photoNegItem) { applyOp(PHOTONEG_OP); }

            else if (mi==RGBThreshItem) { applyOp(RGBTHRESH_OP); }

            repaint();

        }

     

        void handleCCMenu(JMenuItem mi) {

            System.out.println("A connected components menu item was selected.");

            if (mi==CCItem1) { computeConnectedComponents(); }

        }

        void handleHelpMenu(JMenuItem mi){

            System.out.println("A help menu item was selected.");

            if (mi==aboutItem) {

                System.out.println("About: Well this is my program.");

                JOptionPane.showMessageDialog(this,

                    "Image Components, Starter-Code Version.",

                    "About",

                    JOptionPane.PLAIN_MESSAGE);

            }

            else if (mi==helpItem) {

                System.out.println("In case of panic attack, select File: Quit.");

                JOptionPane.showMessageDialog(this,

                    "To load a new image, choose File: Load image...\nFor anything else, just try different things.",

                    "Help",

                    JOptionPane.PLAIN_MESSAGE);

            }

        }

     

        /*

         * Used by Swing to set the size of the JFrame when pack() is called.

         */

        public Dimension getPreferredSize() {

            return new Dimension(w, h+50); // Leave some extra height for the menu bar.

        }

     

        public void paintPanel(Graphics g) {

            g.drawImage(biWorking, 0, 0, null);

        }

     

        public void applyOp(BufferedImageOp operation) {

            operation.filter(biWorking, biFiltered);

            gWorking.drawImage(biFiltered, 0, 0, null);

        }

     

        public void actionPerformed(ActionEvent e) {

            Object obj = e.getSource(); // What Swing object issued the event?

            if (obj instanceof JMenuItem) { // Was it a menu item?

                JMenuItem mi = (JMenuItem)obj; // Yes, cast it.

                JPopupMenu pum = (JPopupMenu)mi.getParent(); // Get the object it's a child of.

                JMenu m = (JMenu) pum.getInvoker(); // Get the menu from that (popup menu) object.

                //System.out.println("Selected from the menu: "+m.getText()); // Printing this is a debugging aid.

     

                if (m==fileMenu)    { handleFileMenu(mi);    return; }  // Handle the item depending on what menu it's from.

                if (m==imageOpMenu) { handleImageOpMenu(mi); return; }

                if (m==ccMenu)      { handleCCMenu(mi);      return; }

                if (m==helpMenu)    { handleHelpMenu(mi);    return; }

            } else {

                System.out.println("Unhandled ActionEvent: "+e.getActionCommand());

            }

        }

     

     

        // Use this to put color information into a pixel of a BufferedImage object.

        void putPixel(BufferedImage bi, int x, int y, int r, int g, int b) {

            int rgb = (r << 16) | (g << 8) | b; // pack 3 bytes into a word.

            bi.setRGB(x,  y, rgb);

        }

     

        // Takes in two pixelID roots and makes the smaller value the parent of the other

        // Assumes the pixelIDs don't equal each other

        void union(int pixelID1, int pixelID2) {

            if(pixelID1 > pixelID2){

                parentID[getYcoord(pixelID1)][getXcoord(pixelID1)] = pixelID2;

            } else {

                parentID[getYcoord(pixelID2)][getXcoord(pixelID2)] = pixelID1;

            }

        }

     

        // returns ID of the root parent of pixelID

        int find(int pixelID) {

            if(parentID[getYcoord(pixelID)][getXcoord(pixelID)] == -1) {

                return pixelID;

            } else {

                return find(parentID[getYcoord(pixelID)][getXcoord(pixelID)]);

            }

        }

        // edges are pixels next to each other with same color

        // go through each pixel starting from lower left, check up and right for edges

        // if the find is different, perform union

        // h is number of rows, w is number of columns

        void computeConnectedComponents() {

            int unionCalls = 0;

            int count = 0;

            parentID = new int[h][w];

            IDcoordinates = new HashMap <Integer, ArrayList<Integer>>();

            for (int i = 0; i < h; i++) {

                for (int j = 0; j < w; j++) {

                    parentID[i][j] = -1;

                    ArrayList<Integer> tempList = new ArrayList<Integer>();

                    tempList.add(j);

                    tempList.add(i);

                    IDcoordinates.put(i * w + j, tempList);

                }

            }

            for (int i = 0; i < h; i++) {

                for (int j = 0; j < w; j++) {

                    if(i + 1 < h) {

                        if(biWorking.getRGB(j,i) == biWorking.getRGB(j, i +1)) {

                            if(find(i * w + j) != find((i+1) * w + j)) {

     

                                union(find(i * w + j), find((i+1) * w + j));

                                unionCalls += 1;

                            }

                        }

                    }

                    if(j + 1 < w) {

                        if(biWorking.getRGB(j,i) == biWorking.getRGB(j+1, i)) {

                            //System.out.println("found right edge");

                            if(find(i * w + j) != find(i * w + j + 1)) {

                                union(find(i * w + j), find(i * w + j + 1));

                                unionCalls += 1;

                            }

                        }

                    }

                }

            }

            System.out.println("The number of times that the method UNION was called for this image is: " + unionCalls);

     

            Hashtable<Integer, Integer> componentNumber = new Hashtable<Integer, Integer>();

            for (int i = 0; i < h; i++) {

                for (int j = 0; j < w; j++) {

                    if(parentID[i][j] == -1) {

                        //count += 1;

                        componentNumber.put(i * w + j, count);

                        count += 1;

                    }

                }

            }

            ProgressiveColors pc = new ProgressiveColors();

            for (int i = 0; i < h; i ++) {

                for (int j = 0; j < w; j ++) {

                    int root = find(i * w + j);

                    int k = componentNumber.get(root);

                    int [] rgb = pc.progressiveColor(k);

                    int r = rgb[0];

                    int g = rgb[1];

                    int b = rgb[2];

                    putPixel(biWorking, j, i, r, g, b);

                }

            }

            repaint();

            System.out.println("The number of connected components in this image is: " + count);

        }

     

        // pixelID for the pixel at (x,y) is y*w + x

        int getXcoord(int pixelID) {

            return IDcoordinates.get(pixelID).get(0);

        }

     

        // IDcoordinates contains map from pixels to coordinates

        int getYcoord(int pixelID) {

            return IDcoordinates.get(pixelID).get(1);

        }

     

        /* This main method can be used to run the application. */

        public static void main(String s[]) {

            appInstance = new ImageComponents();

        }

    }

