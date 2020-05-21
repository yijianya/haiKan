package com.haikan.demo.util;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.alibaba.fastjson.JSONObject;
import com.haikan.demo.web.portraitController;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.sun.jna.NativeLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class haiKanUtil {

    private static final Logger log = LoggerFactory.getLogger(haiKanUtil.class);
	// SDK
	public static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
	static HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo;

	// 人脸
	public String m_FDID; // 人脸库ID
	public boolean m_isSupportFDLib; // 是否支持人脸功能
	public List<HCNetSDK.NET_DVR_FDLIB_PARAM> m_FDLibList;
	public NativeLong m_lUploadHandle;
	public NativeLong m_UploadStatus;
	public String m_picID;
	public String strModeData;

	// 登陆
	private static final String addr = "32.24.38.40";
	private static final short host = 8000;
	private static final String user = "admin";
	private static final String password = "a12345678";
	public NativeLong lUserID;// 用户句柄
	NativeLong lAlarmHandle;// 报警布防句柄
	NativeLong lListenHandle;// 报警监听句柄

	// 布防
	HCNetSDK.FMSGCallBack fMSFCallBack;// 报警回调函数实现

	public static void main(String[] args) {
		// TODO Auto-generated method stub


		// int res = login();
		/*haiKanUtil s = new haiKanUtil();
		s.login();
		s.GetFaceCapabilities();
		s.SearchFDLib();
		s.getFaceData();*/
		//getXML();

	}

	// 登陆
	public void login() {

		m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
		lUserID = hCNetSDK.NET_DVR_Login_V30(addr, host, user, password, m_strDeviceInfo);

		long userID = lUserID.longValue();
		if (userID == -1) {
			int iErr = hCNetSDK.NET_DVR_GetLastError();
            log.info("注册失败，错误号：" + iErr);
		} else {
            log.info("注册成功");
		}
	}

	// 人脸能力集获取
	public void GetFaceCapabilities() {
		HCNetSDK.NET_DVR_XML_CONFIG_INPUT inBuf = new HCNetSDK.NET_DVR_XML_CONFIG_INPUT();
		inBuf.dwSize = inBuf.size();
		String url = "GET /ISAPI/Intelligent/FDLib/capabilities\r\n";

		HCNetSDK.BYTE_ARRAY ptrUrl = new HCNetSDK.BYTE_ARRAY(url.length());
		System.arraycopy(url.getBytes(), 0, ptrUrl.byValue, 0, url.length());
		ptrUrl.write();
		inBuf.lpRequestUrl = ptrUrl.getPointer();
		inBuf.dwRequestUrlLen = url.length();

		HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT outBuf = new HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT();
		outBuf.dwSize = outBuf.size();
		HCNetSDK.BYTE_ARRAY ptrOutByte = new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_DATA_LEN);
		outBuf.lpOutBuffer = ptrOutByte.getPointer();
		outBuf.dwOutBufferSize = HCNetSDK.ISAPI_DATA_LEN;
		outBuf.write();

		if (hCNetSDK.NET_DVR_STDXMLConfig(lUserID, inBuf, outBuf)) {
			log.info("-----获取人脸能力集成功");
			m_isSupportFDLib = true;
		} else {
			int code = hCNetSDK.NET_DVR_GetLastError();
			// JOptionPane.showMessageDialog(null, "获取人脸能力集失败: " + code);
			log.info("获取人脸能力集失败: " + code);
			m_isSupportFDLib = false;
		}
	}

	// 查询人脸库
	public boolean SearchFDLib() {
		try {
			if (m_isSupportFDLib) {
				// 返回true，说明支持人脸
				HCNetSDK.NET_DVR_XML_CONFIG_INPUT struInput = new HCNetSDK.NET_DVR_XML_CONFIG_INPUT();
				struInput.dwSize = struInput.size();

				String str = "GET /ISAPI/Intelligent/FDLib\r\n";
				HCNetSDK.BYTE_ARRAY ptrUrl = new HCNetSDK.BYTE_ARRAY(HCNetSDK.BYTE_ARRAY_LEN);
				System.arraycopy(str.getBytes(), 0, ptrUrl.byValue, 0, str.length());
				ptrUrl.write();
				struInput.lpRequestUrl = ptrUrl.getPointer();
				struInput.dwRequestUrlLen = str.length();

				HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT struOutput = new HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT();
				struOutput.dwSize = struOutput.size();

				HCNetSDK.BYTE_ARRAY ptrOutByte = new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_DATA_LEN);
				struOutput.lpOutBuffer = ptrOutByte.getPointer();
				struOutput.dwOutBufferSize = HCNetSDK.ISAPI_DATA_LEN;

				HCNetSDK.BYTE_ARRAY ptrStatusByte = new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_STATUS_LEN);
				struOutput.lpStatusBuffer = ptrStatusByte.getPointer();
				struOutput.dwStatusSize = HCNetSDK.ISAPI_STATUS_LEN;
				struOutput.write();

				if (hCNetSDK.NET_DVR_STDXMLConfig(lUserID, struInput, struOutput)) {
					String xmlStr = struOutput.lpOutBuffer.getString(0);

					// dom4j解析xml
					Document document = DocumentHelper.parseText(xmlStr);
					// 获取根节点元素对象
					Element FDLibBaseCfgList = document.getRootElement();

					// 同时迭代当前节点下面的所有子节点
					Iterator<Element> iterator = FDLibBaseCfgList.elementIterator();
					while (iterator.hasNext()) {
						HCNetSDK.NET_DVR_FDLIB_PARAM tmp = new HCNetSDK.NET_DVR_FDLIB_PARAM();
						Element e = iterator.next();
						Iterator<Element> iterator2 = e.elementIterator();
						while (iterator2.hasNext()) {
							Element e2 = iterator2.next();
							if (e2.getName().equals("id")) {
								String id = e2.getText();
								tmp.dwID = Integer.parseInt(id);
							}
							if (e2.getName().equals("name")) {
								tmp.szFDName = e2.getText();
								log.info(e2.getText());
							}
							if (e2.getName().equals("FDID")) {
								log.info("FDID:" + e2.getText());
								tmp.szFDID = e2.getText();
							}
						}
						m_FDLibList.add(tmp);
					}

				} else {
					log.info("创建人脸库失败: " + hCNetSDK.NET_DVR_GetLastError());
					return false;
				}
			} else {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	// 获取人脸库图片数据
	public List<String> getFaceData() {

        List<String> stringList = new ArrayList<String>();
		// input
		HCNetSDK.NET_DVR_XML_CONFIG_INPUT struInput = new HCNetSDK.NET_DVR_XML_CONFIG_INPUT();
		struInput.dwSize = struInput.size();

		String str = "POST /ISAPI/Intelligent/FDLib/FCSearch\r\n";
		HCNetSDK.BYTE_ARRAY ptrUrl = new HCNetSDK.BYTE_ARRAY(HCNetSDK.BYTE_ARRAY_LEN);
		System.arraycopy(str.getBytes(), 0, ptrUrl.byValue, 0, str.length());
		ptrUrl.write();
		struInput.lpRequestUrl = ptrUrl.getPointer();
		struInput.dwRequestUrlLen = str.length();
		int pos = 1;
		boolean isQuit = false;

		String strInBuffer = getXML();
		HCNetSDK.BYTE_ARRAY ptrByte = new HCNetSDK.BYTE_ARRAY(10 * HCNetSDK.BYTE_ARRAY_LEN);
		ptrByte.byValue = strInBuffer.getBytes();
		ptrByte.write();
		struInput.lpInBuffer = ptrByte.getPointer();
		struInput.dwInBufferSize = strInBuffer.length();
		struInput.write();

		// output
		HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT struOutput = new HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT();
		struOutput.dwSize = struOutput.size();

		HCNetSDK.BYTE_ARRAY ptrOutByte = new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_DATA_LEN);
		struOutput.lpOutBuffer = ptrOutByte.getPointer();
		struOutput.dwOutBufferSize = HCNetSDK.ISAPI_DATA_LEN;

		HCNetSDK.BYTE_ARRAY ptrStatusByte = new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_STATUS_LEN);
		struOutput.lpStatusBuffer = ptrStatusByte.getPointer();
		struOutput.dwStatusSize = HCNetSDK.ISAPI_STATUS_LEN;
		struOutput.write();

		if (hCNetSDK.NET_DVR_STDXMLConfig(lUserID, struInput, struOutput)) {

			String xmlStr = struOutput.lpOutBuffer.getString(0);
			struOutput.clear();
            //log.info(xmlStr);
			Document document = null;
			try {
				document = DocumentHelper.parseText(xmlStr);
			} catch (DocumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Element FCSearchResult = document.getRootElement();
			Iterator<Element> iterator = FCSearchResult.elementIterator();
			while (iterator.hasNext()) {
				Element e = iterator.next();
				if (e.getName().equals("responseStatusStrg")) {
					if (e.getText().equals("MORE")) {
						isQuit = false;
					} else {
						isQuit = true;
					}
				}
				if (e.getName().equals("numOfMatches")) {
					pos += Integer.parseInt(e.getText());
					log.info("pos：" + pos);
				}
				if (e.getName().equals("totalMatches")) {
					if (isQuit) {
						log.info("搜索到的总数是：" + e.getText());
					}
				}
				if (e.getName().equals("MatchList")) {
					Iterator<Element> iterator2 = e.elementIterator();
					while (iterator2.hasNext()) {
						Element e2 = iterator2.next();
						Iterator<Element> iterator3 = e2.elementIterator();
						while (iterator3.hasNext()) {
							Element e3 = iterator3.next();
							if (e3.getName().equals("snapTime")){
                                log.info("抓拍时间：" + e3.getText());
                            }
							if (e3.getName().equals("facePicURL")) {
                                log.info("facePicURL：" + e3.getText());
                                try {
                                    if(!StringUtils.isBlank(e3.getText())){
                                        String faceUrl = e3.getText();
                                        int star = faceUrl.indexOf("?name=");
                                        int end = faceUrl.indexOf("&");
                                        faceUrl = faceUrl.substring(star+6,end);
                                        log.info(faceUrl);
                                        stringList.add(this.picBase64(faceUrl));
                                    }
                                }catch (Exception e1){
                                    e1.printStackTrace();
                                }
							}
						}
					}
				}
			}
		}
		return stringList;
	}

	//base64图片
    private String picBase64(String name) {
        String ss = "";
        HCNetSDK.NET_DVR_PIC_PARAM struGetPicParam = new HCNetSDK.NET_DVR_PIC_PARAM();

        HCNetSDK.BYTE_ARRAY struFileName = new HCNetSDK.BYTE_ARRAY(64);
        struFileName.read();
        struFileName.byValue = name.getBytes();
        struFileName.write();

        HCNetSDK.BYTE_ARRAY struPicData = new HCNetSDK.BYTE_ARRAY(999999);
        struPicData.read();

        struGetPicParam.pDVRFileName = struFileName.getPointer();
        struGetPicParam.pSavedFileBuf = struPicData.getPointer();
        struGetPicParam.dwBufLen = 999999;
        struGetPicParam.write();

        if (false == hCNetSDK.NET_DVR_GetPicture_V50(lUserID, struGetPicParam)) {
            int iErr = hCNetSDK.NET_DVR_GetLastError();
            log.info("NET_DVR_GetPicture_V50失败，错误号：" + iErr);
        } else {
            struPicData.read();
            try {
                BASE64Encoder encoder = new BASE64Encoder();
                ss = encoder.encode(struPicData.byValue);
                //Base64ToImage(ss,"D:/haikan/"+name+".jpg");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                struPicData.clear();
            }
        }
        return ss;
    }

    //获取xml报文
    private static String getXML(){
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd");
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -1);
        String days_ago = ft.format(calendar1.getTime());

        StringBuilder stringBuilder = new StringBuilder("<FCSearchDescription><searchID>00000000-0000-0000-0000-000000000000</searchID><searchResultPosition>0</searchResultPosition><maxResults>20</maxResults><snapStartTime>");
        stringBuilder.append(days_ago).append("T00:00:00Z</snapStartTime><snapEndTime>").append(ft.format(new Date()));
        stringBuilder.append("T23:59:59Z</snapEndTime><similarity></similarity><FDIDList><FDID>0</FDID></FDIDList></FCSearchDescription>");
        return stringBuilder.toString();
    }

    //获取当日及之前的人脸url
    public JSONObject getfacePicURL(){
        hCNetSDK.NET_DVR_Init();
        JSONObject json = new JSONObject ();
        json.put("success",false);
        List<String> stringList = new ArrayList<String>();
        try {
            this.login();
            if (lUserID == null || lUserID.intValue() == -1) {
                json.put("msg","登陆出错！");
                return json;
            }
            this.GetFaceCapabilities();
            this.SearchFDLib();
            stringList = this.getFaceData();

            json.put("success",true);
            json.put("data",stringList);
            json.put("msg","获取数据成功!");
        } catch (Exception e) {
            e.printStackTrace();
            json.put("msg","系统异常！");
        }	finally {
            hCNetSDK.NET_DVR_Logout(lUserID);
            hCNetSDK.NET_DVR_Cleanup();
            log.info("=========================查询结束==================");
        }

        return json;
    }

}
