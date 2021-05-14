package org.litetokens.studio;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Tab;
import lombok.Getter;
import lombok.Setter;
import org.spongycastle.util.encoders.Hex;
import org.litetokens.studio.solc.SolidityCompiler;
import org.springframework.beans.factory.annotation.Value;
import org.litetokens.studio.utils.FormatCode;
import org.litetokens.studio.walletserver.WalletClient;
import javafx.scene.Scene;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:studio.properties")
public class ShareData {

    private static final String[] testAccountAddress = {
            "LL1J63ZJN5WQdhY9zofQzCEEmyfUKGNnSt",
            "LepKYAcpmiG3VtQtjyNsDzgMdfoMjNGmDd",
            "LLZeRDCp7aQzdX9pGFwG29NZpKKuoSMpYu",
            "LgMPE9TnSyx6pQ27JY2Z1wS4FrSuDD7UFT",
            "Lf5mbtivajjs2onvfC9CDd83RNsaoPak8z"
    };
    private static final String[] testAccountPrivateKey = {
            "a2ebbdda09a2bc1c083b2b38c0bcd238293d51f98b51f3ccc5ac1f3ec06ab16b",
            "e5a02406388e02d1a3ac7f4be6fa3213bbf01d65e8ca3fbb90dc3c9b750b5aba",
            "d424eff5c2974fd0fa942dcc07819fe24b1f0f5c197014d91efa49ef9ef5f70d",
            "96ce60f7c745b288073b368281e053f744fc50cafd04888c9c46364115f56488",
            "90066dc6917a94b94f15eb36af1b49b8492980594e24aa429dfa05ea11b4dbff"
    };
    public static class NetWorkEnvironment {
        public String url;
        public int port;

        public NetWorkEnvironment(String url, int port){
            this.url = url;
            this.port = port;
        }
    }

    @Getter
    @Setter
    public static String localRpcIp = "127.0.0.1";

    @Getter
    @Setter
    public static int localRpcPort = 16669;

    @Getter
    @Setter
    public static String testNetRpcIp = "47.254.144.25";

    @Getter
    @Setter
    public static int testNetRpcPort = 50051;

    @Getter
    @Setter
    public static String mainNetRpcIp = "54.236.37.243";

    @Getter
    @Setter
    public static int mainNetRpcPort = 50051;


    @Getter
    @Setter
    public static String currentRpcIp = localRpcIp;

    @Getter
    @Setter
    public static int currentRpcPort = localRpcPort;


    @Getter
    @Setter
    public static String currentEnvironment = "Local LVM";

    public static Boolean enableOptimize = true;
    public static long XLT_SUN_UNIT = 1_000_000;

    @Value("${studio.autocomplete}")
    public static boolean enableAutoComplete;

    public static SimpleStringProperty newAccount = new SimpleStringProperty();
    public static SimpleStringProperty newNetwork = new SimpleStringProperty();
    public static final LinkedHashMap<String, String> testAccount = new LinkedHashMap<>();
    public static final LinkedHashMap<String, NetWorkEnvironment> saved_network = new LinkedHashMap<>();//network name: ip:port
    public static WalletClient wallet = new WalletClient(Hex.decode(testAccountPrivateKey[0]));



    //合约的编译结果
    public static SimpleObjectProperty<SolidityCompiler.Result> currentSolidityCompilerResult = new SimpleObjectProperty<>();
    private static HashMap<String, SolidityCompiler.Result> solidityCompilerResultMap = new HashMap<>();

    //当前合约文件中，被选中的合约（合约文件中可能包含多份合约）
    public static SimpleStringProperty currentContractName = new SimpleStringProperty();

    //当前正在编辑的合约文件
    public static SimpleStringProperty currentContractFileName = new SimpleStringProperty();
    //新建的合约文件
    public static SimpleStringProperty newContractFileName = new SimpleStringProperty();
    //所有的合约文件列表
    public static SimpleSetProperty<String> allContractFileName = new SimpleSetProperty<>(FXCollections.observableSet(new TreeSet<>()));

    //所有的交易历史记录
    //包括不上链的交易：TransactionExtension
    //包括上链的交易：Transaction
    //包括错误交易信息：ErrorInfo
    public static HashMap<String, TransactionHistoryItem> transactionHistory = new HashMap<>();

    // Add transaction
    public static SimpleStringProperty addTransactionAction = new SimpleStringProperty();
    public static SimpleStringProperty debugTransactionAction = new SimpleStringProperty();
    public static SimpleStringProperty openContract = new SimpleStringProperty();
    public static SimpleStringProperty deleteContract = new SimpleStringProperty();
    public static SimpleStringProperty openContractFileName = new SimpleStringProperty();
    public static Tab currentContractTab;

    public static String currentAccount;
    public static String currentValue;

    //自动编译
    public static SimpleBooleanProperty isAutoCompile = new SimpleBooleanProperty();
    //当前合约文件源代码
    public static SimpleStringProperty currentContractSourceCode = new SimpleStringProperty();


    // 文本错误信息
    public static List<FormatCode.MissInfo> missInfoList = new ArrayList<>();

    public static SimpleObjectProperty<Scene> sceneObjectProperty = new SimpleObjectProperty();

    public static boolean isScrolling = false;
    //public static int currentPara = 0;
    private ShareData() {

    }

    private static void setSavedNetWork(){

        saved_network.put("Local LVM", new NetWorkEnvironment("127.0.0.1", 16669));
        saved_network.put("Test Net", new NetWorkEnvironment(testNetRpcIp,testNetRpcPort));
        saved_network.put("Main Net", new NetWorkEnvironment(mainNetRpcIp, mainNetRpcPort));
        try {
            File fXmlFile = new File(Paths.get(System.getProperty("user.home"), "LitetokensStudio", "record", "network.xml").toString());
            if( !fXmlFile.exists() ) return;
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("network");

            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);


                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;
                    saved_network.put(eElement.getAttribute("id").trim(), new NetWorkEnvironment(eElement.getElementsByTagName("ip").item(0).getTextContent().trim(),
                            Integer.parseInt(eElement.getElementsByTagName("port").item(0).getTextContent().trim())));

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static {
        if (testAccountAddress.length == testAccountPrivateKey.length) {
            for (int i = 0; i < testAccountAddress.length; ++i) {
                testAccount.put(testAccountAddress[i], testAccountPrivateKey[i]);
            }
        }

        setSavedNetWork();
        currentRpcIp = saved_network.get(currentEnvironment).url;
        currentRpcPort = saved_network.get(currentEnvironment).port;
    }

    public static SolidityCompiler.Result getSolidityCompilerResult(String contractName) {
        return solidityCompilerResultMap.get(contractName);
    }

    public static void setSolidityCompilerResult(String contractName, SolidityCompiler.Result solidityCompilerResult) {
        solidityCompilerResultMap.put(contractName, solidityCompilerResult);
        currentSolidityCompilerResult.set(solidityCompilerResult);
    }



}