/**
 * 
 */
package cn.sh.ideal.controller;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.util.NodeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;

import cn.sh.ideal.model.LoginRes;
import cn.sh.ideal.util.HttpUtils;


/**
 * @project springboot
 * @Package springboot
 * @typeName SpringBootDemo
 * @author Stanley Zhou
 * @Description:  
 * @date 2016年5月9日 下午1:56:20
 * @version 
 */
@RestController
@EnableAutoConfiguration
@ComponentScan(basePackages={"cn.sh.ideal"})
public class DelegateReply implements EmbeddedServletContainerCustomizer {
	
	@Autowired
	private HttpUtils client;
	
	private static String _t;
	private static String varName = "";
    private static String varValue = "";
    
	
    @RequestMapping(value ="/test")
    @ResponseBody
	public String repqqqly() {
    	return "test success................";
    }
    
    @RequestMapping(value ="/reply", method = RequestMethod.POST)
    @ResponseBody
	public String reply(@RequestBody String param) {
		try {
			
			System.out.println("reply"+client);
			JSONObject replyData = JSON.parseObject(param);
			String url = "http://club.jd.com/comment/saveProductCommentReply.action?commentId="+ replyData.getString("guid") +"&content=" + URLEncoder.encode(URLEncoder.encode(replyData.getString("content"), "utf-8"), "utf-8") +"&parentId=" + replyData.getString("id");
			Map<String, String> headerParams = new HashMap<>();
            System.out.println("cookies = " + client.getCookies());
            headerParams.put("Cookie", client.getCookies());
            Header[] headers = HttpUtils.getHeaders(headerParams);
			String data = client.sendByGet(url, headers, false);
			return data;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }
  
    
    
    @RequestMapping(value ="/login", method = RequestMethod.POST)
    @ResponseBody
    public boolean login(@RequestBody String param) {
        try {
        	
        	System.out.println("login"+client);
        	//String userName, String pwd, String authCode
        	JSONObject loginData = JSON.parseObject(param);
        	
            Map<String, String> headerParams = new HashMap<>();
            System.out.println("login method cookies = " + client.getCookies());
            
//          headerParams.put("Cookie", client.getCookies());
            Header[] headers = HttpUtils.getHeaders(headerParams);
            StringBuilder loginUrl = new StringBuilder("https://passport.jd.com/uc/loginService?ltype=logout&version=2015");
            loginUrl.append("&_t=").append(_t)
                    .append("&loginname=")
                    .append(loginData.getString("account"))
                    .append("&nloginpwd=&loginpwd=")
                    .append(loginData.getString("password"))
                    .append("&authcode=")
                    .append(loginData.getString("authCode"))
                    .append("&").append(varName).append("=").append(varValue);
//          System.out.println(loginUrl.toString());
            String res = client.sendByGet(loginUrl.toString(), headers, true);
            res = res.substring(1);
            res = res.substring(0, res.length()-1);
            Gson gson = new Gson();
            LoginRes loginRes = gson.fromJson(res, LoginRes.class);
            System.out.println("登陆京东账户 : "+loginData.getString("account")+" ,结果 :"+(JSON.toJSONString(loginRes)));
            return !StringUtils.isEmpty(loginRes.getSuccess());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
	}
    
    
    @RequestMapping(value ="/authCodeImg", method = RequestMethod.POST)
    @ResponseBody
	public String getAuthCodeAndInit() throws Exception {
		client.setCookies("");
		String html = loginIndex();
		if (html != null && !"".equals(html)) {
			init(html);
			return getAuthCode(html);
		}
		return null;
	}
	
	private String loginIndex() throws Exception {
		String url = "https://passport.jd.com/new/login.aspx?ReturnUrl=" + escape("http://www.jd.com");
		return client.sendByGet(url, true);
	}
	
	private static void init(String html) throws Exception {
        Parser parser = new Parser(html);
        NodeClassFilter classFilter = new NodeClassFilter(InputTag.class);
        NodeList list = parser.parse(classFilter);
        int count = list.size();	// 属性有type的结点个数
        for(int i=0; i < count; i++) {
            // 拿到隐藏域内容
            InputTag input = (InputTag) list.elementAt(i);
            String type = input.getAttribute("type");
            String id = input.getAttribute("id");
            if ("hidden".equals(type)) {
                if ("token".equals(id)) {
                    _t = input.getAttribute("value");
                } 
                if (StringUtils.isEmpty(id)) {
                	varName = input.getAttribute("name");
                    varValue = input.getAttribute("value");
                }
            }
        }
	}
	
	/**
	 * 获取页面中的验证码域
	 * @param html
	 * @return
	 */
	public static String getAuthCode(String html) {
		String src = "";
		try {
//			System.out.println(html);
			Parser parser = new Parser(html);
			NodeClassFilter classFilter = new NodeClassFilter(ImageTag.class);
            NodeList imageNodeList = parser.parse(classFilter);
            int imageNodeCount = imageNodeList.size();	// 图片结点个数
            for(int i=0; i < imageNodeCount; i++) {
                // 拿到图片结点内容
            	ImageTag image = (ImageTag) imageNodeList.elementAt(i);
                String id = image.getAttribute("id");
                if ("JD_Verification1".equals(id)) {
                	src = "http:" + image.getAttribute("src2").replaceAll("amp;", "");
                }
            }
		} catch (Exception e) {
			src = "";
		} 
		return src;
	}
	
	private static String escape(String str) {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByExtension("js");
        try {
            return (String) engine.eval(String.format("escape('%s')", str));
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return "";
    }
    
	 
    public static void main(String[] args) {
        SpringApplication.run(DelegateReply.class, args);
    }

	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		container.setPort(8089);		
	}
	    
}