package mfc.model;

import gnu.io.CommPort;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import java.util.logging.Logger;
import javax.swing.Timer;

public class MFC implements ActionListener {
    
    private String commID;
    
    private String serialNum;
    
    private BufferedInputStream in;
    
    private BufferedOutputStream out;
    
    private CommPort com;
    
    private final Object lock;
    
    private final PropertyChangeSupport propChSup;
    
    private final Timer timer;
    
    private static final Logger log;
    
    private static final int[] GET_FLOW_COMMAND;
    
    private static final int[] CLOSE_VALVE_COMMAND;
    
    private static final int[] OPEN_VALVE_COMMAND;
    
    private static final int[] CONTROL_VALVE_COMMAND;
    
    private static final int[] SET_NEW_FLOW_COMMAND;
    
    private static final int[] GET_STATUS_COMMAND;
    
    private static final int[] HANDSHAKE_COMMAND;
    
    static {
        
        log = Logger.getLogger(MFC.class.getName());
        
        GET_FLOW_COMMAND = new int[8];
        GET_FLOW_COMMAND[0] = 0x11;
        GET_FLOW_COMMAND[1] = 0x00;
        GET_FLOW_COMMAND[2] = 0x00;
        GET_FLOW_COMMAND[3] = 0x00;
        GET_FLOW_COMMAND[4] = 0x00;
        GET_FLOW_COMMAND[5] = 0x00;
        GET_FLOW_COMMAND[6] = 0x00;
        GET_FLOW_COMMAND[7] = 0x01;
        
        CLOSE_VALVE_COMMAND = new int[8];
        CLOSE_VALVE_COMMAND[0] = 0x20;
        CLOSE_VALVE_COMMAND[1] = 0x00;
        CLOSE_VALVE_COMMAND[2] = 0x02;
        CLOSE_VALVE_COMMAND[3] = 0x00;
        CLOSE_VALVE_COMMAND[4] = 0x00;
        CLOSE_VALVE_COMMAND[5] = 0x00;
        CLOSE_VALVE_COMMAND[6] = 0x00;
        CLOSE_VALVE_COMMAND[7] = 0x01;
        
        GET_STATUS_COMMAND = new int[8];
        GET_STATUS_COMMAND[0] = 0x01;
        GET_STATUS_COMMAND[1] = 0x00;
        GET_STATUS_COMMAND[2] = 0x00;
        GET_STATUS_COMMAND[3] = 0x00;
        GET_STATUS_COMMAND[4] = 0x00;
        GET_STATUS_COMMAND[5] = 0x00;
        GET_STATUS_COMMAND[6] = 0x00;
        GET_STATUS_COMMAND[7] = 0x01;
        
        OPEN_VALVE_COMMAND = new int[8];
        OPEN_VALVE_COMMAND[0] = 0x20;
        OPEN_VALVE_COMMAND[1] = 0x00;
        OPEN_VALVE_COMMAND[2] = 0x01;
        OPEN_VALVE_COMMAND[3] = 0x00;
        OPEN_VALVE_COMMAND[4] = 0x00;
        OPEN_VALVE_COMMAND[5] = 0x00;
        OPEN_VALVE_COMMAND[6] = 0x00;
        OPEN_VALVE_COMMAND[7] = 0x01;
        
        CONTROL_VALVE_COMMAND = new int[8];
        CONTROL_VALVE_COMMAND[0] = 0x20;
        CONTROL_VALVE_COMMAND[1] = 0x00;
        CONTROL_VALVE_COMMAND[2] = 0x00;
        CONTROL_VALVE_COMMAND[3] = 0x00;
        CONTROL_VALVE_COMMAND[4] = 0x00;
        CONTROL_VALVE_COMMAND[5] = 0x00;
        CONTROL_VALVE_COMMAND[6] = 0x00;
        CONTROL_VALVE_COMMAND[7] = 0x01;
        
        SET_NEW_FLOW_COMMAND = new int[8];
        SET_NEW_FLOW_COMMAND[0] = 0x25;
        SET_NEW_FLOW_COMMAND[1] = 0x00;
        SET_NEW_FLOW_COMMAND[2] = 0x00;
        SET_NEW_FLOW_COMMAND[3] = 0x00;
        SET_NEW_FLOW_COMMAND[4] = 0x00;
        SET_NEW_FLOW_COMMAND[5] = 0x00;
        SET_NEW_FLOW_COMMAND[6] = 0x00;
        SET_NEW_FLOW_COMMAND[7] = 0x01;
        
        HANDSHAKE_COMMAND = new int[8];
        HANDSHAKE_COMMAND[0] = 0x19;
        HANDSHAKE_COMMAND[1] = 0x00;
        HANDSHAKE_COMMAND[2] = 0x00;
        HANDSHAKE_COMMAND[3] = 0x00;
        HANDSHAKE_COMMAND[4] = 0x00;
        HANDSHAKE_COMMAND[5] = 0x00;
        HANDSHAKE_COMMAND[6] = 0x00;
        HANDSHAKE_COMMAND[7] = 0x01;
    }
    
    public MFC(CommPort com, BufferedInputStream in, BufferedOutputStream out) 
            throws IOException {
        
        boolean closed;
        boolean successful;
        String ID;
        
        this.in = in;
        this.out = out;
        this.com = com;
        ID = (com.getName()).substring(com.getName().indexOf("COM"));
        this.commID = ID;
        this.lock = new Object();
        this.propChSup = new PropertyChangeSupport(this);
        
        log.fine("Creating MFC at " + ID);
        
        successful = tryToMakeConnection();
        if (!successful) {
            log.warning(this.serialNum + ": failed to make connection!");
            throw new IOException("Failed to make connection");
        }
        closed = this.closeMFCValve();
        if (!closed) {
            log.warning(this.serialNum + ": failed to close the valve!");
            throw new IOException(this.serialNum + ": failed to close the valve!");
        }
        this.timer = new Timer(2000, this);
        log.finer(this.serialNum + ": initialized and started timer");
        
    }
    
    public String getCommID() {
        
        log.finest(this.serialNum + ": sending commId");
        
        return this.commID;
        
    }
    
    public String getSerialNum() {
        
        log.finest(this.serialNum + ": sending serial number");
        
        return this.serialNum;
        
    }
    
//    public double getCurrentFlow() throws IOException {
//        
//        double flow;
//        
//        flow = getFlowFromMFC();
//        
//        return flow;
//        
//    }
    
    public boolean closeValve() throws IOException {
        
        boolean closed;
        
        closed = closeMFCValve();
        
        return closed;
        
    }
    
    public boolean openValve() throws IOException {
        
        boolean opened;

        opened = openMFCValve();

        return opened;
        
    }
    
    public boolean setValveInControlMode() throws IOException {
        
        boolean control;

        control = setMFCValveInControlMode();

        return control;
        
    }
    
    public boolean setNewFlow(double flowInPercents) throws IOException {
        
        boolean successful;
        
        successful = setMFCNewFlow(flowInPercents);
        
        return successful;
        
    }
    
    public void close() throws IOException {
        
        log.fine(this.serialNum + ": closing connection with MFC");
        
        synchronized (lock) {
            try {
                this.closeMFCValve();
            } catch (IOException e) {
                log.warning(this.serialNum + ": failed to close MFC valve!");
            } finally {
                this.in.close();
                this.out.close();
                this.com.close();
            }
        }
        
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        
        this.propChSup.addPropertyChangeListener(listener);
        
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        
        this.propChSup.removePropertyChangeListener(listener);
        
    }

    private double getFlowFromMFC() throws IOException {
        
        double flow;
        int[] response;
        boolean checkSumIsOK;
        
        log.finer(this.serialNum + ": getting flow from MFC...");
        
        synchronized (lock) {
            sendCommandToMFC(MFC.GET_FLOW_COMMAND);
            response = getMFCResponse();
            
            Formatter f;
            f = new Formatter(Locale.US);
            for (int i = 0; i < response.length; i++) {
                f.format("%02x\t", response[i]);
            }
            
            checkSumIsOK = checkCheckSum(response);
            if (checkSumIsOK) {
                log.finest(this.serialNum + ": check sum of returned message is ok");
                flow = decodeFlow(response);
            } else {
                log.severe(this.serialNum + ": error in check sum! "
                        + "Bytes available to read from BufferedInputStream: "
                        + in.available() + ". "
                                + "Response from MFC: " + f.toString());
                flow = Double.NaN;
            }
        }
        
        return flow;
        
    }

    private void sendCommandToMFC(int[] command) throws IOException {
        
        int[] checkSum;
        
        Formatter f;
        f = new Formatter(Locale.US);
        for (int i = 0; i < command.length; i++) {
            f.format("%02x\t", command[i]);
        }
        log.finest(this.serialNum + ": sending command to MFC: " + f.toString());
        
        checkSum = calculateCheckSum(command);
        
        for (int i = 0; i < command.length; i++) {
            this.out.write(command[i]);
        }
        for (int i = 0; i < checkSum.length; i++) {
            this.out.write(checkSum[i]);
        }
        out.flush();
        
    }

    private int[] calculateCheckSum(int[] command) {
        
        int[] checkSum;
        int sum;
        
        
        
        checkSum = new int[2];
        sum = 0x00;
        
        for (int i = 0; i < 8; i++) {
            sum = sum + command[i];
        }
        
        checkSum[0] = (int)(sum / 256);
        checkSum[1] = sum % 256;
        
        Formatter f;
        f = new Formatter(Locale.US);
        for (int i = 0; i < checkSum.length; i++) {
            f.format("%02x\t", checkSum[i]);
        }
        log.finest(this.serialNum + ": calculating check sum for command: " + f.toString());
        
        return checkSum;
        
    }

    private int[] getMFCResponse() throws IOException {
        
        int[] response;
        
        log.finest(this.serialNum + ": getting MFC response...");
        
        long millis = System.currentTimeMillis();
        while (in.available() < 10) { // wait for mfc to send the responce
            if (System.currentTimeMillis() - millis > 500) {
                break;
            }
        }
        
        response = new int[10];
        
        for (int i = 0; i < response.length; i++) {
            response[i] = in.read();
        }
        
        Formatter f;
        f = new Formatter(Locale.US);
        for (int i = 0; i < response.length; i++) {
            f.format("%02x\t", response[i]);
        }
        log.finest(this.serialNum + ": MFC response: " + f.toString());
        
        return response;
        
    }

    private boolean checkCheckSum(int[] response) {
        
        boolean isOK;
        int sum;
        int checkSum;
        
        sum = 0;

        for (int i = 0; i < 8; i++) {
            sum = sum + response[i];
        }
        
        Formatter f;
        f = new Formatter(Locale.US);
        f.format("%02x\t%02x", (int)(sum / 256), sum % 256);
        log.finest(this.serialNum + ": calculated check sum: " + f.toString());
        
        checkSum = response[8] * (int) Math.pow(16, 2) + response[9];
        isOK = checkSum == sum;

        return isOK;
        
    }

    private double decodeFlow(int[] response) {
        
        double flow;
        int[] binFlow;
        int decFlow;
        
        binFlow = new int[16];
        decFlow = response[2] * (int)Math.pow(16, 2) + response[3];
        flow = 0;
        
        for (int i = 0; i < 16; i++) {
            binFlow[i] = decFlow % 2;
            decFlow = (int) (decFlow / 2);
        }
        for (int i = 0; i < 15; i++) {
            flow = flow + binFlow[i] * Math.pow(2, i);
        }
        flow = flow / 100.0;
        if (binFlow[15] == 1) {
            flow = -flow;
        }
        log.finest(this.serialNum + ": decoded flow from MFC: " + String.valueOf(flow));
        
        return flow;
        
    }

    private boolean closeMFCValve() throws IOException {
        
        boolean closed;
        int[] response;
        boolean checkSumIsOK;
        String valveStatus;
        
        log.finer(this.serialNum + ": closing MFC valve...");

        synchronized (lock) {
            sendCommandToMFC(MFC.CLOSE_VALVE_COMMAND); // ask MFC to close the valve
            response = getMFCResponse();
            checkSumIsOK = checkCheckSum(response);
            if (checkSumIsOK) {
                log.finest(this.serialNum + ": check sum is ok. Getting MFC status...");
                sendCommandToMFC(MFC.GET_STATUS_COMMAND); // ask MFC's status
                response = getMFCResponse();
                checkSumIsOK = checkCheckSum(response);
                if (checkSumIsOK) {
                    log.finest(this.serialNum + ": check sum is ok");
                    valveStatus = getMFCValveStatus(response);
                    if (valveStatus.equals("closed")) {
                        log.finest(this.serialNum + ": valve was closed successfully");
                        closed = true;
                    } else {
                        log.warning(this.serialNum + ": valve was not closed! "
                                + "Valve status: " + valveStatus);
                        closed = false;
                    }
                } else {
                    Formatter f;
                    f = new Formatter(Locale.US);
                    for (int i = 0; i < response.length; i++) {
                        f.format("%02x\t", response[i]);
                    }
                    log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                    closed = false;
                }
            } else {
                Formatter f;
                f = new Formatter(Locale.US);
                for (int i = 0; i < response.length; i++) {
                    f.format("%02x\t", response[i]);
                }
                log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                closed = false;
            }
        }
        
        return closed;
        
    }

    private String getMFCValveStatus(int[] response) {
        
        String valveStatus;
        int[] binStatus;
        int decStatus;
        
        binStatus = new int[8];
        decStatus = response[1];
        
        for (int i = 0; i < 8; i++) {
            binStatus[i] = decStatus % 2;
            decStatus = (int)(decStatus / 2);
        }
        if (binStatus[2] == 0 && binStatus[3] == 0) {
            valveStatus = "control";
        } else if (binStatus[2] == 1 && binStatus[3] == 0) {
            valveStatus = "opened";
        } else if (binStatus[2] == 0 && binStatus[3] == 1) {
            valveStatus = "closed";
        } else {
            valveStatus = "opened";
        }
        
        log.finest(this.serialNum + ": decoding valve status: " + String.valueOf(binStatus[2]) + 
                String.valueOf(binStatus[3]) + ": " + valveStatus);
        
        return valveStatus;
        
    }

    private boolean openMFCValve() throws IOException {
        
        boolean opened;
        int[] response;
        boolean checkSumIsOK;
        String valveStatus;
        
        log.finer(this.serialNum + ": opening MFC valve...");

        synchronized (lock) {
            sendCommandToMFC(MFC.OPEN_VALVE_COMMAND); // ask MFC to open the valve
            response = getMFCResponse();
            checkSumIsOK = checkCheckSum(response);
            if (checkSumIsOK) {
                log.finest(this.serialNum + ": check sum is ok. Getting MFC status...");
                sendCommandToMFC(MFC.GET_STATUS_COMMAND); // ask MFC's status
                response = getMFCResponse();
                checkSumIsOK = checkCheckSum(response);
                if (checkSumIsOK) {
                    log.finest(this.serialNum + ": check sum is ok.");
                    valveStatus = getMFCValveStatus(response);
                    if (valveStatus.equals("opened")) {
                        log.finest(this.serialNum + ": valve was opened successfully.");
                        opened = true;
                    } else {
                        log.warning(this.serialNum + ": valve was not opened! "
                                + "Valve status: " + valveStatus);
                        opened = false;
                    }
                } else {
                    Formatter f;
                    f = new Formatter(Locale.US);
                    for (int i = 0; i < response.length; i++) {
                        f.format("%02x\t", response[i]);
                    }
                    log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                    opened = false;
                }
            } else {
                Formatter f;
                f = new Formatter(Locale.US);
                for (int i = 0; i < response.length; i++) {
                    f.format("%02x\t", response[i]);
                }
                log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                opened = false;
            }
        }

        return opened;
        
    }

    private boolean setMFCValveInControlMode() throws IOException {
        
        boolean control;
        int[] response;
        boolean checkSumIsOK;
        String valveStatus;
        
        log.finer(this.serialNum + ": settting MFC valve in control mode...");

        synchronized (lock) {
            sendCommandToMFC(MFC.CONTROL_VALVE_COMMAND); // ask MFC to open the valve
            response = getMFCResponse();
            checkSumIsOK = checkCheckSum(response);
            if (checkSumIsOK) {
                log.finest(this.serialNum + ": check sum is ok. Getting MFC status...");
                sendCommandToMFC(MFC.GET_STATUS_COMMAND); // ask MFC's status
                response = getMFCResponse();
                checkSumIsOK = checkCheckSum(response);
                if (checkSumIsOK) {
                    log.finest(this.serialNum + ": check sum is ok");
                    valveStatus = getMFCValveStatus(response);
                    if (valveStatus.equals("control")) {
                        log.finest(this.serialNum + ": valve was set in control mode successfully");
                        control = true;
                    } else {
                        log.warning(this.serialNum + ": valve was not set in control"
                                + " mode! Valve status: " + valveStatus);
                        control = false;
                    }
                } else {
                    Formatter f;
                    f = new Formatter(Locale.US);
                    for (int i = 0; i < response.length; i++) {
                        f.format("%02x\t", response[i]);
                    }
                    log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                    control = false;
                }
            } else {
                Formatter f;
                f = new Formatter(Locale.US);
                for (int i = 0; i < response.length; i++) {
                    f.format("%02x\t", response[i]);
                }
                log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                control = false;
            }
        }

        return control;
        
    }

    private boolean setMFCNewFlow(double flowInPercents) throws IOException {
        
        boolean successful;
        int[] response;
        String valveStatus;
        boolean checkSumIsOK;
        double setFlow;
        int[] newFlowCommand;
        
        log.finer(this.serialNum + ": setting MFC new flow...");
        
        newFlowCommand = getNewFlowCommand((int)(flowInPercents * 100.0));
        
        synchronized (lock) {
            log.info(this.serialNum + "getting MFC status...");
            sendCommandToMFC(MFC.GET_STATUS_COMMAND);
            response = this.getMFCResponse();
            checkSumIsOK = this.checkCheckSum(response);
            if (checkSumIsOK) {
                log.finest(this.serialNum + ": check sum is ok.");
                valveStatus = this.getMFCValveStatus(response);
                if (valveStatus.equals("control")) {
                    log.finest(this.serialNum + ": valve in control mode. Sending "
                            + "new flow to MFC...");
                    this.sendCommandToMFC(newFlowCommand);
                    response = this.getMFCResponse();
                    checkSumIsOK = this.checkCheckSum(response);
                    if (checkSumIsOK) {
                        log.finest(this.serialNum + ": check sum is ok. Requesting "
                                + "set flow from MFC...");
                        this.sendCommandToMFC(MFC.GET_FLOW_COMMAND);
                        response = this.getMFCResponse();
                        checkSumIsOK = this.checkCheckSum(response);
                        if (checkSumIsOK) {
                            log.finest(this.serialNum + ": check sum is ok.");
                            setFlow = decodeSetFlow(response);
                            if (Math.abs(flowInPercents - setFlow) < 0.05) {
                                log.finest(this.serialNum + ": new flow was successfully set.");
                                successful = true;
                            } else {
                                successful = false;
                                Formatter f;
                                f = new Formatter(Locale.US);
                                f.format("New flow was not set. ", null);
                                f.format("Value to set: %3.2f. ", flowInPercents);
                                f.format("Set value from MFC: %3.2f", setFlow);
                                log.warning(f.toString());
                            }
                        } else {
                            Formatter f;
                            f = new Formatter(Locale.US);
                            for (int i = 0; i < response.length; i++) {
                                f.format("%02x\t", response[i]);
                            }
                            log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                            successful = false;
                        }
                    } else {
                        Formatter f;
                        f = new Formatter(Locale.US);
                        for (int i = 0; i < response.length; i++) {
                            f.format("%02x\t", response[i]);
                        }
                        log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                        successful = false;
                    }
                } else {
                    log.warning(this.serialNum + ": cannot set new value. Valve "
                            + "is not in control mode! Valve status: " + valveStatus);
                    successful = false;
                }
            } else {
                Formatter f;
                f = new Formatter(Locale.US);
                for (int i = 0; i < response.length; i++) {
                    f.format("%02x\t", response[i]);
                }
                log.warning(this.serialNum + ": error in check sum! MFC response: " + f.toString());
                successful = false;
            }
        }
        
        return successful;
        
    }

    private int[] getNewFlowCommand(int newFlow) {
        
        int[] newFlowCommand;
        
        log.finest(this.serialNum + ": building SetNewFlow command...");
        
        newFlowCommand = new int[8];
        
        newFlowCommand[0] = MFC.SET_NEW_FLOW_COMMAND[0];
        newFlowCommand[1] = MFC.SET_NEW_FLOW_COMMAND[1];
        newFlowCommand[2] = (int)(newFlow / (int)(Math.pow(16, 2)));
        newFlowCommand[3] = newFlow % (int)(Math.pow(16, 2));
        newFlowCommand[4] = MFC.SET_NEW_FLOW_COMMAND[4];
        newFlowCommand[5] = MFC.SET_NEW_FLOW_COMMAND[5];
        newFlowCommand[6] = MFC.SET_NEW_FLOW_COMMAND[6];
        newFlowCommand[7] = MFC.SET_NEW_FLOW_COMMAND[7];
        
        return newFlowCommand;
        
    }

    private double decodeSetFlow(int[] response) {
        
        double setFlow;
        int decFlow;

        decFlow = response[4] * (int) Math.pow(16, 2) + response[5];
        setFlow = decFlow / 100.0;
        
        log.finest(this.serialNum + ": decoding requested set flow from MFC: " + String.valueOf(setFlow));

        return setFlow;
        
    }

    private boolean tryToMakeConnection() throws IOException {
        
        boolean successful;
        int[] response;
        boolean checkSumIsOK;
        
        log.finer("Trying to make connection with MFC...");
        
        synchronized (lock) {
            this.sendCommandToMFC(MFC.HANDSHAKE_COMMAND);
            response = this.getMFCResponse();
            checkSumIsOK = this.checkCheckSum(response);
            if (checkSumIsOK) {
                log.finest("Check sum is ok. Connection was established successfully.");
                this.serialNum = getMFCSerialNum(response);
                successful = true;
            } else {
                Formatter f;
                f = new Formatter(Locale.US);
                for (int i = 0; i < response.length; i++) {
                    f.format("%02x\t", response[i]);
                }
                log.warning("Error in check sum! MFC response: " + f.toString());
                successful = false;
            }
        }
        
        return successful;
        
    }

    private String getMFCSerialNum(int[] response) {
        
        String serialNum;
        int n;

        n = response[5] * (int) Math.pow(16, 2) + response[6];
        serialNum = String.valueOf(n);
        
        log.finest("MFC serial number: " + serialNum);
        
        return serialNum;
        
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        
        double flow;
        
        try {
            flow = this.getFlowFromMFC();
        } catch (IOException ex) {
            log.warning(this.serialNum + ": failed to get flow from MFC!");
            flow = Double.NaN;
        }
        this.propChSup.firePropertyChange(this.serialNum, 0, flow);
        
    }
    
    public void startTimer() {
        
        this.timer.start();
        
    }

}