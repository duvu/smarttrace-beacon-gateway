package com.TZONE.Bluetooth.Temperature.Model;

import com.TZONE.Bluetooth.Utils.StringConvertUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Update file
 * Created by Forrest on 2017/4/19.
 */
public class FileUpdate {

    private String _Type = "";
    private String _Version = "";
    private int _FileSize = 0;
    private int _PackageNumber = 0;
    private int _CRC16 = 0;
    private List<byte[]> _Bytelist = new ArrayList<>();

    public FileUpdate(String filePath) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        // Get the file object
        Document doc = builder.parse(new File(filePath));
        // Get the root directory
        Element root = doc.getDocumentElement();
        Element appImage = (Element)root.getElementsByTagName("appImage").item(0);
        _Type = appImage.getAttribute("type");
        _Version = appImage.getAttribute("version");
        _FileSize = Integer.parseInt(appImage.getAttribute("size"));
        _PackageNumber = Integer.parseInt(appImage.getAttribute("packagenumber"));
        _CRC16 = Integer.parseInt(appImage.getAttribute("crc"));

        NodeList nodeList = appImage.getElementsByTagName("item");
        _Bytelist.clear();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element item = (Element)nodeList.item(i);
            _Bytelist.add(StringConvertUtil.hexStringToBytes(item.getAttribute("itemdata")));
        }
    }

    public String getType(){ return  _Type;}
    public String getVersion(){ return  _Version;}
    public int getFileSize(){ return  _FileSize;}
    public int getPackageNumber(){ return _PackageNumber;}
    public List<byte[]> getBytelist(){ return _Bytelist;}
    public int get_CRC16(){ return _CRC16;}

}
