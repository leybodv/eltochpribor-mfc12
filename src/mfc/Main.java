package mfc;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JPanel;
import mfc.gui.MFCPanel;
import mfc.model.MFC;

/**
 * Программа для управления РРГ-12 производства ООО "Элточприбор". Программа позволяет
 * управлять несколькими регуляторами. Для связи с регулятором используется протокол
 * "Элточприбор-10М". Для связи через COM порт используется сторонняя библиотека
 * RXTX (http://rxtx.qbang.org/wiki/index.php/Main_Page).
 * 
 * @author Лейбо Д.
 */
public class Main {
    
    /**
     * Панели графического интерфейса для связи с РРГ
     */
    private static ArrayList<MFCPanel> panels;
    
    /**
     * Подключённые РРГ
     */
    private static ArrayList<MFC> mfcs;
    
    private static Logger log;
    
    /**
     * Стандартная скорость связи с РРГ-12
     */
    private static final int DEFAULT_BAUD = 19200;
    
    /**
     * Максимальный поток по азоту для существующих реализаций РРГ-12
     */
    private static final Integer[] AVAILABLE_MAX_FLOWS;
    
    /**
     * Газы, для которых известны коэффициенты перевода максимального потока
     */
    private static final String[] GASES;
    
    private static final String LOG_DIR;
    
    private static final Level LOG_LEVEL;
    
    static {
        AVAILABLE_MAX_FLOWS = new Integer[] 
            {6,
            15,
            60,
            150,
            300,
            600,
            1500,
            3000,
            6000,
            12000,
            15000,
            30000};
        
        GASES = new String[] {
            "N2",
            "Air",
            "NH3",
            "Ar",
            "CO2",
            "CO",
            "He",
            "H2",
            "CH4",
            "O2",
            "AsH3",
            "CS2",
            "CCl4",
            "CF4",
            "Cl2",
            "B2H6",
            "SiH2Cl2",
            "CHF3",
            "CClF3",
            "C2ClF3",
            "GeCl4",
            "HCl",
            "NO",
            "NO2",
            "PH3",
            "SiH4",
            "SiCl4",
            "SF6",
            "SiHCl3"
        };
        
        LOG_DIR = "MFC.logs";
        
        LOG_LEVEL = Level.OFF;
        
        log = Logger.getLogger(Main.class.getName());
    }
    
    /**
     * Метод позволяет запускать программу как отдельное приложение
     * @param args не используются
     */
    public static void main(String[] args) {
        
        JFrame window;
        JPanel container;
        Dimension screenSize;
        Dimension windowSize;
        int windowWidth;
        int windowHeight;
        int screenWidth;
        int screenHeight;
        if (LOG_LEVEL != Level.OFF) {
			setupLogger();
		}
        createAvailableMFCs();
        createPanels();
        applyPreferencesToPanels();
        Locale.setDefault(Locale.US);
        log.info("Setting up window...");
        window = new JFrame("MFC");
        container = new JPanel();
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = screenSize.width;
        screenHeight = screenSize.height;
        
        for (JPanel panel : panels) {
            container.add(panel);
        }
        window.setContentPane(container);
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                
                log.info("Exiting the program...");
                
                Main.savePreferences();
                for (MFC mfc : mfcs) {
                    try {
                        mfc.close();
                    } catch (IOException ex) {
                        log.severe("Unable to close mfc " + mfc.getSerialNum() + "!");
                        System.exit(1);
                    }
                }
                System.exit(0);
                
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

        });
        window.pack();
        windowSize = window.getSize();
        windowWidth = windowSize.width;
        windowHeight = windowSize.height;
        window.setLocation((int)((screenWidth - windowWidth) / 2.0), 
                (int) ((screenHeight - windowHeight) / 2.0));
        window.setVisible(true);
        
    }

    /**
     * Метод пытается связаться с подключёнными к COM портам РРГ
     */
    private static void createAvailableMFCs() {
        
        log.info("Searching for available MFCs...");
        
        ArrayList<MFC> availableMFCs;
        MFC mfc;
        Enumeration ports;
        CommPortIdentifier commID;

        availableMFCs = new ArrayList<MFC>();

        ports = CommPortIdentifier.getPortIdentifiers();
        if (!ports.hasMoreElements()) {
            log.severe("There is no available com ports!");
            System.exit(1);
        }
        while (ports.hasMoreElements()) {
            commID = (CommPortIdentifier) ports.nextElement();
            mfc = getMFCFromPort(commID);
            if (mfc != null) {
                availableMFCs.add(mfc);
            }
        }
        if (availableMFCs.size() > 0) {
            Main.mfcs = availableMFCs;
        } else {
            log.severe("There is no available MFCs!");
            System.exit(1);
        }
        
    }
    
    /**
     * Метод создаёт объект типа MFC после удачной связи с РРГ через указанный порт
     * @param commID идентификатор порта к которому предположительно подключён РРГ
     * @return объект типа MFC, если связь с РРГ установлена удачно или null в 
     * противном случае
     */
    private static MFC getMFCFromPort(CommPortIdentifier commID) {

        log.fine("Trying to connect to MFC @ " + commID.getName() + "...");
        
        MFC mfc;
        CommPort commPort;
        SerialPort serialPort;
        InputStream in;
        OutputStream out;
        BufferedInputStream serialIn;
        BufferedOutputStream serialOut;
        
        commPort = null;

        if (commID.isCurrentlyOwned()) {
            log.warning("Port " + commID.getName() + " is currently in use!");
            mfc = null;
        } else {
            try {
                commPort = commID.open("MFC", 1000);
                log.finer("Sucessfully opened port " + commID.getName());
                if (commPort instanceof SerialPort) {
                    serialPort = (SerialPort) commPort;
                    serialPort.setSerialPortParams(DEFAULT_BAUD, SerialPort.DATABITS_8,
                            SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    in = serialPort.getInputStream();
                    out = serialPort.getOutputStream();
                    serialIn = new BufferedInputStream(in);
                    serialOut = new BufferedOutputStream(out);
                    mfc = new MFC(commPort, serialIn, serialOut);
                } else {
                    log.warning("Port " + commID.getName() + " is not serial!");
                    mfc = null;
                }
            } catch (PortInUseException ex) {
                mfc = null;
                log.severe("Port " + commID.getName() + " is currently in use!");
            } catch (UnsupportedCommOperationException ex) {
                mfc = null;
                log.severe("Unable to set port " + commID.getName() + " parameters!");
                if (commPort != null) {
                    commPort.close();
                }
            } catch (IOException ex) {
                mfc = null;
                log.warning("Exception occured during connection with port " + 
                        commID.getName() + "!\n" + ex.getMessage());
                if (commPort != null) {
                    commPort.close();
                }
            }
        }

        return mfc;

    }

    /**
     * Метод создаёт панели графического интерфейса для каждого подключённого РРГ
     */
    private static void createPanels() {
        
        log.info("Creating GUI panels for MFCs...");
        
        ArrayList<MFCPanel> panels;
        MFCPanel panel;
        
        panels = new ArrayList<MFCPanel>();
        
        for (MFC mfc : Main.mfcs) {
            panel = new MFCPanel(mfc, Main.AVAILABLE_MAX_FLOWS, Main.GASES);
            panels.add(panel);
        }
        
        Main.panels = panels;
        
    }

    private static void applyPreferencesToPanels() {
        
        log.info("Applying preferences to GUI panels...");
        
        try {
            String pathName;
            Preferences root;
            Preferences node;
            
            pathName = "/mfc";
            root = Preferences.userRoot();
            if (!root.nodeExists(pathName)) {
                return;
            }
            node = root.node(pathName);
            for (MFCPanel panel : panels) {
                String selectedMaxFlow = node.get("mfc." + 
                        panel.getMFCSerialNum() + ".maxflow", null);
                if (selectedMaxFlow != null) {
                    int flow = Integer.parseInt(selectedMaxFlow);
                    panel.selectMaxFlow(flow);
                }
                String selectedGas = node.get("mfc." + panel.getMFCSerialNum() +
                        ".gas", null);
                if (selectedGas != null) {
                    panel.selectGas(selectedGas);
                }
            }
        } catch (Exception ex) {
            log.warning("Exception was thrown during applying preferences!");
        }
        
    }

    private static void savePreferences() {
        
        log.fine("Saving preferences...");
        
        try {
            String pathName;
            Preferences root;
            Preferences node;
            
            pathName = "/mfc";
            root = Preferences.userRoot();
            node = root.node(pathName);
            for (MFCPanel panel : panels) {
                node.put("mfc." + panel.getMFCSerialNum() + ".maxflow", 
                        String.valueOf(panel.getSelectedMaxFlow()));
                node.put("mfc." + panel.getMFCSerialNum() + ".gas", 
                        panel.getSelectedGas());
            }
        } catch (Exception ex) {
            log.warning("Error occured while saving preferences...");
        }
        
    }

    private static void setupLogger() {
        
        Logger logger;
        File logDir;
        FileHandler fileHandler;
        ConsoleHandler consoleHandler;
        SimpleFormatter simpleFormatter;
        
        LogManager.getLogManager().reset(); // disable loggers to write to the console
        logger = Logger.getLogger(Main.class.getPackage().getName());
        logDir = new File(Main.LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        try {
            fileHandler = new FileHandler(LOG_DIR + "\\" + LocalDateTime.now().
                    format(DateTimeFormatter.ofPattern("uuuuMMdd_HHmmss")) + ".log");
            consoleHandler = new ConsoleHandler();
            simpleFormatter = new SimpleFormatter();
            fileHandler.setFormatter(simpleFormatter);
            consoleHandler.setFormatter(simpleFormatter);
            logger.addHandler(fileHandler);
            logger.addHandler(consoleHandler);
            consoleHandler.setLevel(Level.WARNING);
            fileHandler.setLevel(LOG_LEVEL);
            logger.setLevel(LOG_LEVEL);
        } catch (Exception ex) {
            System.err.println("Error occured during setting up logging handler\n" + ex.getMessage());
        }
        
    }
    
}