package com.rebuildIndexes;

import com.mysql.jdbc.CommunicationsException;
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * @author : jingo
 * @version V1.0
 * @Project: RebuildIndexes
 * @Package main.java.com.lddsm
 * @Description: TODO
 * @date Date : 2019年12月10日 14:33
 */
public class MainApp extends JFrame {

    private static Logger log = LoggerFactory.getLogger(MainApp.class);

    Connection conn = null;
    PreparedStatement pst = null;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    IndexWriter indexWriter = null;
    IndexWriterConfig config = null;

    JLabel ipLabel, usLabel, pwLabel, portLabel, moveLabel, dbNameLabel, barLabel, analyzeComboBoxLabel, systemComboBoxLabel, versionComboBoxLabel;
    JTextField ipText, usText, portText, newFilePathText, dbNameText, pwText;
    JButton moveBtn, okBtn;
    JProgressBar bar;
    JComboBox analyzeComboBox, systemComboBox, versionComboBox;

    String uuid = "";// UUID
    String USER = "lddsm"; // 账号
    String PASSWORD = "lddsm";// 密码
    String DATABASENAME = "lddsmdb_clouddoc_150915";// 数据库名称
    String DATABASEPORT = "3606";// 数据库端口
    String DATABASEIP = "localhost";// 数据库地址
    String STOREPATH = "";// 用户选择存储地址
    String LDDSM_STOREPATH = ""; // 云文档存储地址
    String LDFBS_STOREPATH = ""; // 智能备份存储地址
    String URL = "";// mysql访问地址，拼接上述参数
    double quantityInExecution = 0;// 索引创建完毕的计数君
    int percentage = 0;// 进度条百分比数值

    public MainApp() {
        init();
    }

    public static void main(String[] args) {
        MainApp mainApp = new MainApp();
    }

    public void init() {
        final JFrame jf = new JFrame("绿盘索引重建工具");
        jf.setSize(470, 530);
        jf.setLocationRelativeTo(null);
        jf.setResizable(false);
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(240, 210));
        panel.setLayout(null);
        ipLabel = new JLabel("数据库地址:   ");
        ipLabel.setBounds(50, 20, 100, 25);
        ipText = new JTextField();
        ipText.setText(DATABASEIP);
        ipText.setBounds(130, 20, 250, 25);
        dbNameLabel = new JLabel("数据库名称:   ");
        dbNameLabel.setBounds(50, 60, 100, 25);
        dbNameText = new JTextField();
        dbNameText.setBounds(130, 60, 250, 25);
        dbNameText.setText(DATABASENAME);
        usLabel = new JLabel("数据库账号:   ");
        usLabel.setBounds(50, 100, 100, 25);
        usText = new JTextField();
        usText.setText(USER);
        usText.setBounds(130, 100, 250, 25);
        pwLabel = new JLabel("数据库密码:   ");
        pwLabel.setBounds(50, 140, 100, 25);
        pwText = new JTextField();
        pwText.setText(PASSWORD);
        pwText.setBounds(130, 140, 250, 25);
        portLabel = new JLabel("数据库端口:   ");
        portLabel.setBounds(50, 180, 100, 25);
        portText = new JTextField();
        portText.setText(DATABASEPORT);
        portText.setBounds(130, 180, 250, 25);
        systemComboBoxLabel = new JLabel("请选择系统");
        systemComboBoxLabel.setBounds(50, 220, 250, 25);
        systemComboBox=new JComboBox();
        systemComboBox.addItem("云文档");
        systemComboBox.addItem("智能备份");
        systemComboBox.setBounds(130, 220, 250, 25);
        versionComboBoxLabel = new JLabel("请选择版本");
        versionComboBoxLabel.setBounds(50, 260, 250, 25);
        versionComboBox=new JComboBox();
        versionComboBox.addItem("旧版数据库");
        versionComboBox.addItem("新版数据库");
        versionComboBox.setBounds(130, 260, 250, 25);
        analyzeComboBoxLabel = new JLabel("请选择分词:   ");
        analyzeComboBoxLabel.setBounds(50, 300, 250, 25);
        analyzeComboBox=new JComboBox();
        analyzeComboBox.addItem("SmartChineseAnalyzer");
        analyzeComboBox.addItem("IKAnalyzer");
        analyzeComboBox.setBounds(130, 300, 250, 25);
        moveLabel = new JLabel("重建库位置:   ");
        moveLabel.setBounds(50, 340, 100, 25);
        newFilePathText = new JTextField();
        newFilePathText.setEditable(false);
        newFilePathText.setBounds(130, 340, 250, 25);
        moveBtn = new JButton("选择");
        moveBtn.setBounds(393, 340, 60, 25);
        moveBtn.addMouseListener(new MouseAdapter() { // 添加鼠标点击事件
            public void mouseClicked(MouseEvent event) {
            fileMovePath(new JButton());
            }
        });

        barLabel = new JLabel("重建库进度:   ");
        barLabel.setBounds(50, 380, 250, 25);
        bar = new JProgressBar();
        bar.setStringPainted(true);
        bar.setBounds(130, 380, 250, 25);
        bar.setMinimum(0);
        bar.setMaximum(100);

        okBtn = new JButton("       立刻开始        ");
        okBtn.setBounds(165, 430, 150, 35);
        okBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ipText.getText().isEmpty() || usText.getText().isEmpty() || pwText.getText().isEmpty()
                        || portText.getText().isEmpty() || dbNameText.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "请填写完整数据库信息!", "提示", JOptionPane.ERROR_MESSAGE);
                } else if (newFilePathText.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "请选择索引库重建地址!", "提示", JOptionPane.ERROR_MESSAGE);
                } else {
                    okBtn.setEnabled(false);
                    DATABASEIP = ipText.getText();
                    USER = usText.getText();
                    PASSWORD = pwText.getText();
                    DATABASEPORT = portText.getText();
                    STOREPATH = newFilePathText.getText();
                    LDDSM_STOREPATH = STOREPATH + "//com.lddsm.dao.bean.NodeBase";
                    LDFBS_STOREPATH = STOREPATH + "//com.lddsm.dao.bean.LdfbsSearchNode";
                    DATABASENAME = dbNameText.getText();
                    URL = "jdbc:mysql://" + DATABASEIP + ":" + DATABASEPORT + "/" + DATABASENAME + "?useUnicode=true&amp;characterEncoding=utf-8";
                    percentage = 1;// 进度先到1再说，不要给用户一种卡死的感觉
                    quantityInExecution = 0; // 初始化进度为0
                    // 计数君开始！
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            new ATask().start();
                        }
                    }).start();
                    // 索引重建开始！
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String system = systemComboBox.getSelectedItem().toString();
                            String analy = analyzeComboBox.getSelectedItem().toString();
                            if(analy.equals("SmartChineseAnalyzer")){
                                config = new IndexWriterConfig(Version.LUCENE_31, new SmartChineseAnalyzer(Version.LUCENE_31));// 智能中文分词器
                            } else if(analy.equals("IKAnalyzer")){
                                config = new IndexWriterConfig(Version.LUCENE_31, new IKAnalyzer()); //IK分词器
                            }
                            if(system.equals("云文档")){
                                // 创建系统默认云文档文件夹
                                File lddsmFile = new File(LDDSM_STOREPATH);
                                if (!lddsmFile.exists()) {
                                    lddsmFile.mkdirs();
                                }
                                rebuildLddsmIndexes();
                            }
                            if(system.equals("智能备份")){
                                // 创建系统默认云文档文件夹
                                File ldfbsFile = new File(LDFBS_STOREPATH);
                                if (!ldfbsFile.exists()) {
                                    ldfbsFile.mkdirs();
                                }
                                rebuildLdfbsIndexes();
                            }
                        }
                    }).start();
                }
            }
        });

        panel.add(ipLabel);
        panel.add(ipText);
        panel.add(dbNameLabel);
        panel.add(dbNameText);
        panel.add(usLabel);
        panel.add(usText);
        panel.add(pwLabel);
        panel.add(pwText);
        panel.add(portLabel);
        panel.add(portText);
        panel.add(moveLabel);
        panel.add(newFilePathText);
        panel.add(moveBtn);
        panel.add(systemComboBoxLabel);
        panel.add(systemComboBox);
        panel.add(versionComboBoxLabel);
        panel.add(versionComboBox);
        panel.add(analyzeComboBox);
        panel.add(analyzeComboBoxLabel);
        panel.add(barLabel);
        panel.add(bar);
        panel.add(okBtn);
        jf.setContentPane(panel);
        jf.setVisible(true);
        jf.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * 文件库迁移路径
     */
    public void fileMovePath(JButton moveBtn) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showOpenDialog(moveBtn);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File newFilePath = chooser.getSelectedFile();
            newFilePathText.setText(newFilePath.getPath());
        }
    }

    /**
     *重建云文档索引
     *
     * @param: []
     * @return: void
     * @author: caiyf
     * @date: 2019-12-18
     */
    public void rebuildLddsmIndexes() {
        long startTime = System.currentTimeMillis(); // 获取开始时间
        try {
            FSDirectory directory = FSDirectory.open(new File(LDDSM_STOREPATH));
            IndexWriter.isLocked(directory);
            indexWriter = new IndexWriter(directory, config);
            IndexWriter.isLocked(directory);
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            gotoRubuildLddsm();
            indexWriter.commit();
            long endTime = System.currentTimeMillis(); // 获取结束时间
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            String gapTime = formatter.format(endTime - startTime - TimeZone.getDefault().getRawOffset());
            JOptionPane.showMessageDialog(null, "索引库重建成功!用时：" + gapTime, "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (CommunicationsException e) {
            log.error("索引库重建失败-CommunicationsException!", e);
            JOptionPane.showMessageDialog(null, "请填写正确的数据库地址!", "提示", JOptionPane.ERROR_MESSAGE);
        } catch (MySQLSyntaxErrorException e) {
            log.error("索引库重建失败-MySQLSyntaxErrorException!", e);
            JOptionPane.showMessageDialog(null, "请填写正确的数据库名称!", "提示", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            log.error("索引库重建失败-Exception!" + uuid, e);
            JOptionPane.showMessageDialog(null, "索引库重建失败!", "提示", JOptionPane.ERROR_MESSAGE);
        } finally {
            close();
        }
    }

    public void gotoRubuildLddsm() throws Exception {
        String documentCountSql = "SELECT\n" +
                "\tcount(*) as num\n" +
                "FROM\n" +
                "\tdsm_node_base t1\n" +
                "RIGHT JOIN dsm_node_document t2 ON t1.NBS_UUID = t2.NBS_UUID\n" +
                "RIGHT JOIN dsm_node_document_text t3 ON t2.NBS_UUID = t3.NBS_UUID\n" +
                "WHERE t1.NDC_ISFOLDER = \"F\"";
        pst = conn.prepareStatement(documentCountSql);
        ResultSet resultSet = pst.executeQuery();
        resultSet.next();
        double documentNum = resultSet.getInt("num");
        String version = versionComboBox.getSelectedItem().toString();
        String folderCountSql = "SELECT\n" +
                "\tCOUNT(*) AS num\n" +
                "FROM\n" +
                "\tdsm_node_base t1\n" +
                "RIGHT JOIN dsm_node_folder t2 ON t1.NBS_UUID = t2.NBS_UUID\n" +
                "WHERE t1.NDC_ISFOLDER = \"T\"";
        pst = conn.prepareStatement(folderCountSql);
        resultSet = pst.executeQuery();
        resultSet.next();
        double folderNum = resultSet.getInt("num");
        double num = documentNum + folderNum;

        String documentSql = "SELECT\n" +
                "\t*\n" +
                "FROM\n" +
                "\tdsm_node_base t1\n" +
                "RIGHT JOIN dsm_node_document t2 ON t1.NBS_UUID = t2.NBS_UUID\n" +
                "RIGHT JOIN dsm_node_document_text t3 ON t2.NBS_UUID = t3.NBS_UUID\n" +
                "WHERE t1.NDC_ISFOLDER = \"F\"";
        pst = conn.prepareStatement(documentSql);
        resultSet = pst.executeQuery();
        while (resultSet.next()) {
            Document document = new Document();
            uuid = resultSet.getString("NBS_UUID");
            document.add(new Field("uuid", uuid, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

            String name = resultSet.getString("NBS_NAME").toLowerCase();
            document.add(new Field("name", name, Field.Store.YES, Field.Index.NOT_ANALYZED));

            String parent = resultSet.getString("NBS_PARENT");
            document.add(new Field("parent", parent, Field.Store.YES, Field.Index.NOT_ANALYZED));

            if(version.equals("旧版数据库")){
                // 旧版作者
                String author = resultSet.getString("NBS_AUTHOR");
                document.add(new Field("author", String.valueOf(author), Field.Store.YES, Field.Index.NOT_ANALYZED));
            }else{
                // 新版作者
                long USR_ID = resultSet.getLong("NBS_AUTHOR");
                String findUserByPk = "SELECT * FROM dsm_user where USR_ID = "+ USR_ID;
                pst = conn.prepareStatement(findUserByPk);
                ResultSet rs = pst.executeQuery();
                rs.next();
                String author = rs.getString("USR_NAME");
                document.add(new Field("author", String.valueOf(author), Field.Store.YES, Field.Index.NOT_ANALYZED));
            }

            String context = resultSet.getString("NBS_CONTEXT");
            document.add(new Field("context", String.valueOf(context), Field.Store.YES, Field.Index.NOT_ANALYZED));

            String created = sdf.format(resultSet.getDate("NBS_CREATED"));
            document.add(new Field("created", created, Field.Store.YES, Field.Index.NOT_ANALYZED));

            Date lastModifiedStr = resultSet.getDate("NBS_LAST_MODIFIED");
            if (lastModifiedStr != null) {
                String lastModified = sdf.format(lastModifiedStr);
                document.add(new Field("lastModified", lastModified, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }

            document.add(new Field("_hibernate_class", "com.lddsm.dao.bean.NodeDocumentText", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            // 类别
            String mimeType = resultSet.getString("NDC_MIME_TYPE");
            document.add(new Field("mimeType", mimeType, Field.Store.YES, Field.Index.NOT_ANALYZED));
            // MD5
            String checksum = resultSet.getString("NDC_LAST_CHECKSUM");
            if (StringUtils.isNotBlank(checksum)) {
                document.add(new Field("checksum", checksum, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }
            // 是否已建立过索引
            boolean textIndexed = resultSet.getBoolean("NDC_TEXT_INDEXED");
            document.add(new Field("textIndexed", String.valueOf(textIndexed), Field.Store.YES, Field.Index.NOT_ANALYZED));
            // 是否被锁定
            boolean locked = resultSet.getBoolean("NDC_LOCKED");
            document.add(new Field("locked", String.valueOf(locked), Field.Store.YES, Field.Index.NOT_ANALYZED));
            // 路径Id
            long pathid = resultSet.getLong("NDC_LAST_PATHID");
            document.add(new Field("pathid", String.valueOf(pathid), Field.Store.YES, Field.Index.NOT_ANALYZED));
            // 是否被抽取
            boolean textExtracted = resultSet.getBoolean("NDC_TEXT_EXTRACTED");
            document.add(new Field("textExtracted", String.valueOf(textExtracted), Field.Store.YES, Field.Index.NOT_ANALYZED));
            // 语言
            String language = resultSet.getString("NDC_LANGUAGE");
            if (StringUtils.isNotBlank(language)) {
                document.add(new Field("language", language, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }
            // 文字
            String text = resultSet.getString("NDC_TEXT");
            if (StringUtils.isNotBlank(text)) {
                document.add(new Field("text", text, Field.Store.NO, Field.Index.ANALYZED));
            }
            // 是否已经被抽取
            Boolean checkedOut = resultSet.getBoolean("NDC_CHECKED_OUT");
            document.add(new Field("checkedOut", String.valueOf(checkedOut), Field.Store.YES, Field.Index.NOT_ANALYZED));
            quantityInExecution++;
            BigDecimal b1 = new BigDecimal(quantityInExecution);
            BigDecimal b2 = new BigDecimal(num);
            double v = b1.divide(b2, 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
            percentage = (int) v;

            try{
                indexWriter.addDocument(document);
            }catch(Exception e){
                log.error(uuid);
                document.removeField("text");
                indexWriter.addDocument(document);
            }
        }

        String folderSql = "SELECT\n" +
                "\t*\n" +
                "FROM\n" +
                "\tdsm_node_base t1\n" +
                "RIGHT JOIN dsm_node_folder t2 ON t1.NBS_UUID = t2.NBS_UUID\n" +
                "WHERE t1.NDC_ISFOLDER = \"T\"";
        pst = conn.prepareStatement(folderSql);
        resultSet = pst.executeQuery();
        while (resultSet.next()) {
            Document document = new Document();
            uuid = resultSet.getString("NBS_UUID");
            document.add(new Field("uuid", uuid, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

            if(version.equals("旧版数据库")){
                // 旧版作者
                String author = resultSet.getString("NBS_AUTHOR");
                document.add(new Field("author", String.valueOf(author), Field.Store.YES, Field.Index.NOT_ANALYZED));
            }else{
                // 新版作者
                long USR_ID = resultSet.getLong("NBS_AUTHOR");
                String findUserByPk = "SELECT * FROM dsm_user where USR_ID = "+ USR_ID;
                pst = conn.prepareStatement(findUserByPk);
                ResultSet rs = pst.executeQuery();
                rs.next();
                String author = rs.getString("USR_NAME");
                document.add(new Field("author", String.valueOf(author), Field.Store.YES, Field.Index.NOT_ANALYZED));
            }

            String name = resultSet.getString("NBS_NAME");
            document.add(new Field("name", name, Field.Store.YES, Field.Index.NOT_ANALYZED));

            String parent = resultSet.getString("NBS_PARENT");
            document.add(new Field("parent", parent, Field.Store.YES, Field.Index.NOT_ANALYZED));

            String context = resultSet.getString("NBS_CONTEXT");
            document.add(new Field("context", String.valueOf(context), Field.Store.YES, Field.Index.NOT_ANALYZED));

            String created = sdf.format(resultSet.getDate("NBS_CREATED"));
            document.add(new Field("created", created, Field.Store.YES, Field.Index.NOT_ANALYZED));

            Date lastModifiedStr = resultSet.getDate("NBS_LAST_MODIFIED");
            if (lastModifiedStr != null) {
                String lastModified = sdf.format(lastModifiedStr);
                document.add(new Field("lastModified", lastModified, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }

            document.add(new Field("_hibernate_class", "com.lddsm.dao.bean.NodeFolder", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            quantityInExecution++;
            BigDecimal b1 = new BigDecimal(quantityInExecution);
            BigDecimal b2 = new BigDecimal(num);
            double v = b1.divide(b2, 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
            percentage = (int) v;
            indexWriter.addDocument(document);
        }
    }

    /**
     *重建云文档索引
     *
     * @param: []
     * @return: void
     * @author: caiyf
     * @date: 2019-12-18
     */
    public void rebuildLdfbsIndexes() {
        long startTime = System.currentTimeMillis(); //获取开始时间
        try {
            FSDirectory directory = FSDirectory.open(new File(LDFBS_STOREPATH));
            IndexWriter.isLocked(directory);
            indexWriter = new IndexWriter(directory, config);
            IndexWriter.isLocked(directory);
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            gotoRubuildLdfbs();
            indexWriter.commit();
            long endTime = System.currentTimeMillis(); //获取结束时间
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            String gapTime = formatter.format(endTime - startTime - TimeZone.getDefault().getRawOffset());
            JOptionPane.showMessageDialog(null, "索引库重建成功!用时：" + gapTime, "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (CommunicationsException e) {
            log.error("索引库重建失败-CommunicationsException!", e);
            JOptionPane.showMessageDialog(null, "请填写正确的数据库地址!", "提示", JOptionPane.ERROR_MESSAGE);
        } catch (MySQLSyntaxErrorException e) {
            log.error("索引库重建失败-MySQLSyntaxErrorException!", e);
            JOptionPane.showMessageDialog(null, "请填写正确的数据库名称!", "提示", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            log.error("索引库重建失败-Exception!" + uuid, e);
            JOptionPane.showMessageDialog(null, "索引库重建失败!", "提示", JOptionPane.ERROR_MESSAGE);
        } finally {
            close();
        }
    }

    public void gotoRubuildLdfbs() throws Exception {
        String searchNodeCountSql = "SELECT count(*) as num FROM ldfbs_search_node;";
        pst = conn.prepareStatement(searchNodeCountSql);
        ResultSet resultSet = pst.executeQuery();
        resultSet.next();
        double num = resultSet.getInt("num");

        String searchNodeSql = "SELECT * FROM ldfbs_search_node;";
        pst = conn.prepareStatement(searchNodeSql);
        resultSet = pst.executeQuery();
        while (resultSet.next()) {
            Document document = new Document();
            // Id
            uuid = resultSet.getString("LSN_ID");
            document.add(new Field("id", uuid, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            // 类别
            document.add(new Field("_hibernate_class", "com.lddsm.dao.bean.LdfbsSearchNode", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            // 是否为智能备份
            Boolean isVirtual = resultSet.getBoolean("IS_VIRTUAL");
            if(isVirtual){
                document.add(new Field("isVirtual",  String.valueOf("true"), Field.Store.YES, Field.Index.NOT_ANALYZED));
            }else{
                document.add(new Field("isVirtual",  String.valueOf("false"), Field.Store.YES, Field.Index.NOT_ANALYZED));
            }
            // 名称
            String name = resultSet.getString("NBS_NAME").toLowerCase();;
            document.add(new Field("name",  name, Field.Store.YES, Field.Index.NOT_ANALYZED));
            // 最后修改时间
            Date lastModifiedStr = resultSet.getDate("NBS_LAST_MODIFIED");
            if (lastModifiedStr != null) {
                String lastModified = sdf.format(lastModifiedStr);
                document.add(new Field("lastModified", lastModified, Field.Store.YES, Field.Index.NOT_ANALYZED));
            }
            Long umId = resultSet.getLong("UM_ID");
            document.add(new Field("umId",  String.valueOf(umId), Field.Store.YES, Field.Index.NOT_ANALYZED));
            quantityInExecution++;
            BigDecimal b1 = new BigDecimal(quantityInExecution);
            BigDecimal b2 = new BigDecimal(num);
            double v = b1.divide(b2, 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
            percentage = (int) v;
            indexWriter.addDocument(document);
        }
    }


    /**
     *关闭资源
     *
     * @param: []
     * @return: void
     * @author: caiyf
     * @date: 2019-12-23
     */
    public void close(){
        percentage = 101;
        okBtn.setEnabled(true);
        try {
            if (pst != null) {
                pst.close();
            }
            if (conn != null) {
                conn.close();
            }
            if (indexWriter != null) {
                indexWriter.close();
            }
            bar.setValue(0);
        } catch (Exception e) {
            log.error("索引库重建finally关闭资源失败!", e);
        }
    }

    class ATask extends Thread {
        int end = 0; // 是否已经结束
        public void run() {
            while (end == 0 && percentage <= 100) {
                if (percentage == 100) {
                    end = 1; // 已经到达过100，是时候结束线程了1
                }
                bar.setValue(percentage);
            }
        }
    }

}
