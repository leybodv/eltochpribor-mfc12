package mfc.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.Locale;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mfc.model.MFC;

/**
 * Панель графического интерфейса с элементами управления РРГ. Объект данного класса
 * также обрабатывает события, связанные с изменением состояния панели, отправляя
 * соответствующие команды подключённому РРГ
 * 
 * @author Лейбо Д.
 */
public class MFCPanel extends JPanel implements ActionListener, ChangeListener,
        ItemListener, PropertyChangeListener {
    
    /**
     * Всплывающий список с возможным максимальным расходом азота в мл/мин
     */
    private JComboBox maxFlowCombo;
    
    /**
     * Ползунок, позволяющий устанавливать расход газа
     */
    private JSlider setupFlowSlider;
    
    /**
     * Всплывающий список с вариантами используемых газов
     */
    private JComboBox gasCombo;
    
    /**
     * Предыдущий газ, который был выбран из выпадающего списка. Используется
     * для пересчёта коэффициента максимального расхода газа
     */
    private String previousGas;
    
    /**
     * Предыдущее состояние клапана, выбранное пользователем. Используется при
     * возникновении ошибки установки нового состояния клапана на РРГ
     */
    private String previousValveState;
    
    /**
     * Кнопка-переключатель соответствующая закрытию клапана РРГ
     */
    private JRadioButton closedButton;
    
    /**
     * Кнопка-переключатель соответствующая открытию клапана РРГ
     */
    private JRadioButton openedButton;
    
    /**
     * Кнопка-переключатель, соответствующая переводу РРГ в режим регулирования
     */
    private JRadioButton controlButton;
    
    /**
     * Текущий поток газа, измеренный РРГ
     */
    private JLabel currentFlow;
    
    /**
     * Текстовое поле для ввода значения потока в мл/мин
     */
    private JTextField flowInSCCM;
    
    /**
     * Текстовое поле для ввода значения потока в %
     */
    private JTextField flowInPercent;
    
    /**
     * Текущий статус РРГ. Устанавливается значение false при сбоях в выполнении
     * команд РРГ
     */
    private JLabel statusMark;
    
    /**
     * РРГ управление которым происходит на данной панели
     */
    private MFC mfc;
    
    private static Logger log;
    
    /**
     * Таблица газов и коэффициентов перевода максимального потока
     */
    private static final Hashtable GAS_COEFFICIENTS;
    
    static {
        
        GAS_COEFFICIENTS = new Hashtable<String, Double>();
        GAS_COEFFICIENTS.put("N2", 1.00);
        GAS_COEFFICIENTS.put("Air", 1.00);
        GAS_COEFFICIENTS.put("NH3", 0.73);
        GAS_COEFFICIENTS.put("Ar", 1.45);
        GAS_COEFFICIENTS.put("AsH3", 0.67);
        GAS_COEFFICIENTS.put("CO2", 0.74);
        GAS_COEFFICIENTS.put("CS2", 0.6);
        GAS_COEFFICIENTS.put("CO", 1.00);
        GAS_COEFFICIENTS.put("CCl4", 0.31);
        GAS_COEFFICIENTS.put("CF4", 0.42);
        GAS_COEFFICIENTS.put("Cl2", 0.86);
        GAS_COEFFICIENTS.put("B2H6", 0.44);
        GAS_COEFFICIENTS.put("SiH2Cl2", 0.4);
        GAS_COEFFICIENTS.put("CHF3", 0.5);
        GAS_COEFFICIENTS.put("CClF3", 0.38);
        GAS_COEFFICIENTS.put("C2ClF3", 0.24);
        GAS_COEFFICIENTS.put("GeCl4", 0.27);
        GAS_COEFFICIENTS.put("He", 1.454);
        GAS_COEFFICIENTS.put("H2", 1.01);
        GAS_COEFFICIENTS.put("HCl", 1.00);
        GAS_COEFFICIENTS.put("CH4", 0.72);
        GAS_COEFFICIENTS.put("NO", 0.99);
        GAS_COEFFICIENTS.put("NO2", 0.74);
        GAS_COEFFICIENTS.put("O2", 1.00);
        GAS_COEFFICIENTS.put("PH3", 0.76);
        GAS_COEFFICIENTS.put("SiH4", 0.6);
        GAS_COEFFICIENTS.put("SiCl4", 0.28);
        GAS_COEFFICIENTS.put("SF6", 0.26);
        GAS_COEFFICIENTS.put("SiHCl3", 0.33);
        
        log = Logger.getLogger(MFCPanel.class.getName());
        
    }
    
    /**
     * Конструктор создаёт панель, добавляет на неё компоненты, используя GroupLayout,
     * запускает таймер.
     * @param mfc объект типа MFC отвечающий за связь с РРГ
     * @param availableMaxFlows вектор с вариантами максимальных расходов по азоту
     * @param gases вектор с возможными используемыми газами
     */
   public MFCPanel(MFC mfc, Integer[] availableMaxFlows, String[] gases) {
       
       log.fine("Creating GUI panel for " + mfc.getSerialNum() + "MFC...");
       
       JLabel header;
       JLabel maxFlowLabel;
       JLabel flowUnits;
       JLabel gasLabel;
       JLabel status;
       JSeparator separator;
       JLabel workingMode;
       ButtonGroup buttonGroup;
       JSeparator separator2;
       JLabel setupFlowLabel;
       JLabel flowUnits2;
       JLabel percent;
       GroupLayout layout;
       
       this.mfc = mfc;
       this.mfc.addPropertyChangeListener(this);
       previousGas = "N2";
       this.previousValveState = "closed";
       header = new JLabel("РРГ " + mfc.getSerialNum() + " @ " + mfc.getCommID());
       maxFlowLabel = new JLabel("Максимальный расход по азоту:");
       maxFlowCombo = new JComboBox(availableMaxFlows);
       maxFlowCombo.addActionListener(this);
       flowUnits = new JLabel("мл/мин");
       gasLabel = new JLabel("Газ: ");
       gasCombo = new JComboBox(gases);
       gasCombo.addActionListener(this);
       status = new JLabel("Статус:");
       statusMark = new JLabel(String.valueOf(true));
       separator = new JSeparator();
       workingMode = new JLabel("Режим работы:");
       buttonGroup = new ButtonGroup();
       closedButton = new JRadioButton("закрыт", true);
       closedButton.addItemListener(this);
       buttonGroup.add(closedButton);
       openedButton = new JRadioButton("открыт", false);
       openedButton.addItemListener(this);
       buttonGroup.add(openedButton);
       controlButton = new JRadioButton("регулирование", false);
       controlButton.addItemListener(this);
       buttonGroup.add(controlButton);
       separator2 = new JSeparator();
       setupFlowLabel = new JLabel("Задать поток:");
       setupFlowSlider = new JSlider(0, (Integer)maxFlowCombo.getSelectedItem() * 100, 0);
       setupSlider(0, (Integer) maxFlowCombo.getSelectedItem() * 100, 0);
       setupFlowSlider.setPaintTicks(true);
       setupFlowSlider.setPaintLabels(true);
       setupFlowSlider.addChangeListener(this);
       flowInSCCM = new JTextField(String.valueOf(setupFlowSlider.getValue() / 100.0), 6);
       flowInSCCM.addActionListener(this);
       flowUnits2 = new JLabel("мл/мин");
       flowInPercent = new JTextField(String.valueOf(setupFlowSlider.getValue() * 100.0
               / setupFlowSlider.getMaximum()), 5);
       flowInPercent.addActionListener(this);
       percent = new JLabel("%");
       currentFlow = new JLabel("Текущий расход: ###.## мл/мин (##.##%)");
       
       layout = new GroupLayout(this);
       this.setLayout(layout);
       this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
       
       layout.setAutoCreateGaps(true);
       layout.setAutoCreateContainerGaps(true);
       
       layout.setHorizontalGroup(
               layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                       .addComponent(header)
                       .addGroup(layout.createSequentialGroup()
                               .addComponent(maxFlowLabel)
                               .addComponent(maxFlowCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                               .addComponent(flowUnits))
                       .addGroup(layout.createSequentialGroup()
                               .addComponent(gasLabel)
                               .addComponent(gasCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                       .addGroup(layout.createSequentialGroup()
                               .addComponent(status)
                               .addComponent(statusMark))
                       .addComponent(separator)
                       .addComponent(workingMode)
                       .addComponent(closedButton)
                       .addComponent(openedButton)
                       .addComponent(controlButton)
                       .addComponent(separator2)
                       .addComponent(setupFlowLabel)
                       .addGroup(layout.createSequentialGroup()
                               .addComponent(setupFlowSlider)
                               .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                       .addGroup(layout.createSequentialGroup()
                                               .addComponent(flowInSCCM, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                               .addComponent(flowUnits2))
                                       .addGroup(layout.createSequentialGroup()
                                               .addComponent(flowInPercent, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                               .addComponent(percent))))
                       .addComponent(currentFlow));
       
       layout.setVerticalGroup(
               layout.createSequentialGroup()
                       .addComponent(header)
                       .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                               .addComponent(maxFlowLabel)
                               .addComponent(maxFlowCombo)
                               .addComponent(flowUnits))
                       .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                               .addComponent(gasLabel)
                               .addComponent(gasCombo))
                       .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                               .addComponent(status)
                               .addComponent(statusMark))
                       .addComponent(separator)
                       .addComponent(workingMode)
                       .addComponent(closedButton)
                       .addComponent(openedButton)
                       .addComponent(controlButton)
                       .addComponent(separator2)
                       .addComponent(setupFlowLabel)
                       .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                               .addComponent(setupFlowSlider)
                               .addGroup(layout.createSequentialGroup()
                                       .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                               .addComponent(flowInSCCM, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                               .addComponent(flowUnits2))
                                       .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                               .addComponent(flowInPercent, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                               .addComponent(percent))))
                       .addComponent(currentFlow));
       
       mfc.startTimer();
       
   }

    @Override
    public void actionPerformed(ActionEvent ae) {
        
        Object source;
        double novelMaxFlow;
        double novelFlowValue;
        double coef;
        String novelGas;
        String text;
        
        source = ae.getSource();
        
        if (source == this.maxFlowCombo) {
            
            log.fine(mfc.getSerialNum() + ": changing max flow rate...");
            
            Formatter formatter;
            String gas;
            
            formatter = new Formatter(Locale.US);
            gas = (String)this.gasCombo.getSelectedItem();
            coef = (double)MFCPanel.GAS_COEFFICIENTS.get(gas);
            novelMaxFlow = ((Integer)maxFlowCombo.getSelectedItem()) * coef;
            
            setupSlider(0, (int)(novelMaxFlow * 100), setupFlowSlider.getValue());
            formatter.format("%2.2f", this.setupFlowSlider.getValue() * 100.0
                    / this.setupFlowSlider.getMaximum());
            this.flowInPercent.setText(formatter.toString());
            this.flowInSCCM.setText(String.valueOf(this.setupFlowSlider.getValue() / 100.0));
            
        } else if (source == this.gasCombo) {
            
            log.fine(mfc.getSerialNum() + ": setting new gas...");
            
            Formatter formatter;
            
            formatter = new Formatter(Locale.US);
            
            novelGas = (String)this.gasCombo.getSelectedItem();
            coef = (double)MFCPanel.GAS_COEFFICIENTS.get(novelGas) / 
                    (double)MFCPanel.GAS_COEFFICIENTS.get(previousGas);
            previousGas = novelGas;
            novelMaxFlow = this.setupFlowSlider.getMaximum() * coef / 100.0;
            setupSlider(0, (int)(novelMaxFlow * 100), (int)(setupFlowSlider.getValue() * coef));
            
            formatter.format("%2.2f", this.setupFlowSlider.getValue() * 100.0 / 
                    this.setupFlowSlider.getMaximum());
            this.flowInPercent.setText(formatter.toString());
            this.flowInSCCM.setText(String.valueOf(this.setupFlowSlider.getValue() / 100.0));
            
        } else if (source == this.flowInSCCM) {
            
            log.fine(mfc.getSerialNum() + ": setting new flow rate...");
            
            Formatter formatter;
            
            formatter = new Formatter(Locale.US);
            
            text = this.flowInSCCM.getText();
            try {
                novelFlowValue = Double.parseDouble(text);
                if (novelFlowValue != setupFlowSlider.getValue() / 100.0) {
                    if (novelFlowValue > setupFlowSlider.getMaximum() / 100.0) {
                        novelFlowValue = setupFlowSlider.getMaximum() / 100.0;
                    } else if (novelFlowValue < setupFlowSlider.getMinimum() / 100.0) {
                        novelFlowValue = setupFlowSlider.getMinimum() / 100.0;
                    }
                    setupSlider(this.setupFlowSlider.getMinimum(), this.setupFlowSlider.getMaximum(), (int) (novelFlowValue * 100));
                    this.flowInSCCM.setText(String.valueOf(this.setupFlowSlider.getValue() / 100.0));
                    formatter.format("%2.2f", novelFlowValue * 10000.0
                            / this.setupFlowSlider.getMaximum());
                    this.flowInPercent.setText(formatter.toString());
                    
                }
            } catch (NumberFormatException ex) {
                log.warning(mfc.getSerialNum() + ": illegal input of flow value!");
                this.flowInSCCM.setText(String.valueOf(setupFlowSlider.getValue() / 100.0));
            }
            
        } else if (source == this.flowInPercent) {
            
            log.fine(mfc.getSerialNum() + ": setting new flow rate...");
            
            Formatter formatter;
            double novelFlowPercentValue;

            formatter = new Formatter(Locale.US);

            text = this.flowInPercent.getText();
            try {
                novelFlowPercentValue = Double.parseDouble(text);
                if (novelFlowPercentValue != setupFlowSlider.getValue() * 100.0 / 
                        this.setupFlowSlider.getMaximum()) {
                    if (novelFlowPercentValue > 100) {
                        novelFlowValue = 100;
                    } else if (novelFlowPercentValue < 0) {
                        novelFlowValue = 0;
                    }
                    setupSlider(this.setupFlowSlider.getMinimum(), this.setupFlowSlider.getMaximum(), (int) (novelFlowPercentValue
                            * this.setupFlowSlider.getMaximum() / 100.0));
                    this.flowInSCCM.setText(String.valueOf(this.setupFlowSlider.getValue() / 100.0));
                    formatter.format("%2.2f", this.setupFlowSlider.getValue() * 100.0
                            / this.setupFlowSlider.getMaximum());
                    this.flowInPercent.setText(formatter.toString());

                }
            } catch (NumberFormatException ex) {
                log.warning(mfc.getSerialNum() + ": illegal input of flow value!");
                this.flowInPercent.setText(String.valueOf(setupFlowSlider.getValue() 
                        * 100.0 / this.setupFlowSlider.getMaximum()));
            }
            
        }
        
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        
        log.fine(mfc.getSerialNum() + ": changing flow rate...");
        
        String flowInSCCMStr;
        String flowInPercentStr;
        Formatter f;
        
        f = new Formatter(Locale.US);
        f.format("%2.2f", this.setupFlowSlider.getValue() / 100.0);
        flowInSCCMStr = f.toString();
        f = new Formatter(Locale.US);
        f.format("%2.2f", this.setupFlowSlider.getValue() * 100.0 / this.setupFlowSlider.getMaximum());
        flowInPercentStr = f.toString();
        this.flowInSCCM.setText(flowInSCCMStr);
        this.flowInPercent.setText(flowInPercentStr);
        
        if (!this.setupFlowSlider.getValueIsAdjusting()) {
            if (controlButton.isSelected()) {
                
                boolean successful;
                
                try {
                    successful = mfc.setNewFlow(this.setupFlowSlider.getValue() * 100.0
                            / this.setupFlowSlider.getMaximum());
                    if (!successful) {
                        throw new IOException();
                    }
                } catch (IOException ex) {
                    this.statusMark.setText(String.valueOf(false));
                    log.severe(mfc.getSerialNum() + ": accidentally lost connection!");
                    JOptionPane.showMessageDialog(this, "Connection with MFC " +
                            mfc.getSerialNum() + " was lost!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        
    }
    
    public String getMFCSerialNum() {
        
        log.finest(mfc.getSerialNum() + ": getting serial number...");
        
        return mfc.getSerialNum();
        
    }
    
    public void selectMaxFlow(int flow) {
        
        log.finest(mfc.getSerialNum() + ": setting max flow rate...");
        
        maxFlowCombo.setSelectedItem(new Integer(flow));
        
    }
    
    public void selectGas(String gas) {
        
        log.finest(mfc.getSerialNum() + ": setting new selected gas...");
        
        gasCombo.setSelectedItem(gas);
        
    }
    
    public int getSelectedMaxFlow() {
        
        log.finest(mfc.getSerialNum() + ": getting currently selected max flow...");
        
        return (int)maxFlowCombo.getSelectedItem();
        
    }
    
    public String getSelectedGas() {
        
        log.finest(mfc.getSerialNum() + ": getting currently selected gas...");
        
        return (String)gasCombo.getSelectedItem();
        
    }

    private void setupSlider(int min, int max, int value) {
        
        log.finer(mfc.getSerialNum() + ": setting up slider value...");
        
        Hashtable labels;
        double oldValueInPercents;
        
        labels = new Hashtable();
        oldValueInPercents = this.setupFlowSlider.getValue() * 100.0 / 
                this.setupFlowSlider.getMaximum();
        
        this.setupFlowSlider.setValueIsAdjusting(true);
        if (this.setupFlowSlider.getMinimum() != min) {
            setupFlowSlider.setMinimum(min);
        }
        if (this.setupFlowSlider.getMaximum() != max) {
            setupFlowSlider.setMaximum(max);
        }
        if (this.setupFlowSlider.getValue() != value) {
            setupFlowSlider.setValue(value);
        }
        if (Math.abs((value * 100.0 / max) - oldValueInPercents) >= 0.005) {
            this.setupFlowSlider.setValueIsAdjusting(false);
        }
        
        setupFlowSlider.setMajorTickSpacing(max);
        setupFlowSlider.setMinorTickSpacing(max / 10);
        labels.put(new Integer(min), new JLabel(String.valueOf(min / 100.0)));
        labels.put(new Integer(max), new JLabel(String.valueOf(max / 100.0)));
        setupFlowSlider.setLabelTable(labels);
        
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        
        Object source;
        
        source = e.getSource();
        
        if (e.getStateChange() == ItemEvent.SELECTED) {
            
            if (source == this.closedButton) {

                boolean closed;
                
                try {
                    closed = mfc.closeValve();
                    if (!closed) {
                        throw new IOException();
                    }
                    this.previousValveState = "closed";
                } catch (IOException ex) {
                    this.closedButton.setSelected(false);
                    if (this.previousValveState.equals("opened")) {
                        this.openedButton.setSelected(true);
                    } else if (this.previousValveState.equals("control")) {
                        this.controlButton.setSelected(true);
                    }
                    this.statusMark.setText(String.valueOf(false));
                    log.severe(mfc.getSerialNum() + ": valve was not closed!");
                    JOptionPane.showMessageDialog(this, "MFC " + mfc.getSerialNum()
                            + " valve was not closed!", "ERROR", JOptionPane.ERROR_MESSAGE);
                }

            } else if (source == this.openedButton) {

                boolean opened;

                try {
                    opened = mfc.openValve();
                    if (!opened) {
                        throw new IOException();
                    }
                    this.previousValveState = "opened";
                } catch (IOException ex) {
                    this.openedButton.setSelected(false);
                    if (this.previousValveState.equals("closed")) {
                        this.closedButton.setSelected(true);
                    } else if (this.previousValveState.equals("control")) {
                        this.controlButton.setSelected(true);
                    }
                    this.statusMark.setText(String.valueOf(false));
                    log.severe(mfc.getSerialNum() + ": valve was not opened!");
                    JOptionPane.showMessageDialog(this, "MFC " + mfc.getSerialNum()
                            + " valve was not opened!", "ERROR", JOptionPane.ERROR_MESSAGE);
                }

            } else if (source == this.controlButton) {

                boolean control;
                boolean flowValueChanged;

                try {
                    control = mfc.setValveInControlMode();
                    if (!control) {
                        throw new IOException();
                    }
                    flowValueChanged = mfc.setNewFlow(this.setupFlowSlider.getValue() * 100.0
                            / this.setupFlowSlider.getMaximum());
                    if (!flowValueChanged) {
                        throw new IOException();
                    }
                    this.previousValveState = "control";
                } catch (IOException ex) {
                    this.controlButton.setSelected(false);
                    if (this.previousValveState.equals("closed")) {
                        this.closedButton.setSelected(true);
                    } else if (this.previousValveState.equals("opened")) {
                        this.openedButton.setSelected(true);
                    }
                    this.statusMark.setText(String.valueOf(false));
                    log.severe(mfc.getSerialNum() + ": valve was not set in control mode!");
                    JOptionPane.showMessageDialog(this, "MFC " + mfc.getSerialNum()
                            + " valve was not set in control mode!", "ERROR", JOptionPane.ERROR_MESSAGE);
                }

            }
        }
        
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce) {
        
        double flow;
        
        flow = (Double)pce.getNewValue();
        
        log.finest(mfc.getSerialNum() + ": property changed. New flow: " + flow);
        
        setCurrentFlow(flow);
        
    }

    private void setCurrentFlow(double flow) {
        
        log.finer(mfc.getSerialNum() + ": showing current flow rate: " + flow);
        
        double flowInSCCM;
        Formatter f;

        f = new Formatter(Locale.US);

        if (Double.isNaN(flow)) {
            currentFlow.setText("Текущий расход: ###.## мл/мин (##.##%)");
            statusMark.setText(String.valueOf(false));
            log.severe(mfc.getSerialNum() + ": connection with MFC lost!");
            JOptionPane.showMessageDialog(this, "Connection with " + 
                    mfc.getSerialNum() + " MFC lost!", "ERROR", JOptionPane.ERROR_MESSAGE);
        } else {
            flowInSCCM = flow * this.setupFlowSlider.getMaximum() / 10000.0;
            f.format("%3.2f", flowInSCCM);
            currentFlow.setText("Текущий расход: " + f.toString() + " мл/мин ("
                    + flow + "%)");
        }
        
    }

}