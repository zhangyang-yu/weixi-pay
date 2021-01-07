package com.zhangyang.weixipay.controller;

import com.github.wxpay.sdk.WXPayUtil;
import com.google.gson.Gson;
import com.zhangyang.weixipay.config.WeixinPayProperties;
import com.zhangyang.weixipay.util.HttpClientUtils;
import com.zhangyang.weixipay.result.R;
import com.zhangyang.weixipay.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wxpay")
public class WXPayCotroller {
    @Autowired
    private WeixinPayProperties weixinPayProperties;
    @GetMapping("/auth/to-pay/{orderNo}")
    public R getWXPay(@PathVariable String orderNo, HttpServletRequest request)
    {
       HttpClientUtils httpClient=new HttpClientUtils("https://api.mch.weixin.qq.com/pay/unifiedorder");
       httpClient.setHttps(true);//发送的请求是https
        //设置发送请求的参数
        Map<String,String> paramMap=new HashMap<>();
        paramMap.put("appid",weixinPayProperties.getAppId());//appid 公众账号
        paramMap.put("mch_id",weixinPayProperties.getPartner());//mch_id 商户号
        paramMap.put("nonce_str",WXPayUtil.generateNonceStr());//nonce_str 保证生成签名无序的随机字符串
        paramMap.put("body","商品");//body 商品描述
        paramMap.put("out_trade_no",orderNo);//out_trade_no 商品的订单号
        paramMap.put("total_fee","1");//total_fee 商品金额  金额单位分
        paramMap.put("spbill_create_ip",request.getRemoteHost());//spbill_create_ip 发送支付请求的客户端地址
        paramMap.put("notify_url",weixinPayProperties.getNotifyUrl());//notify_url 支付结果的通知
        paramMap.put("trade_type","NATIVE");//trade_type 选择支付的类型

        try {
            //创建sign签名和把参数转成xml
            String paramString = WXPayUtil.generateSignedXml(paramMap,weixinPayProperties.getPartnerKey());
            System.out.println(paramString);
            httpClient.setXmlParam(paramString);//把xml类型的参数设置到httpclient中
            httpClient.post();//向微信支付的服务器发送请求
            String content = httpClient.getContent();//获取微信支付服务器的返回值
            //检测sign签名 确保支付安全
            boolean signatureValid = WXPayUtil.isSignatureValid(content, weixinPayProperties.getPartnerKey());
            if(!signatureValid)
            {
                return R.error().message("签名确认失败");
            }
            Map<String, String> WxPayMap = WXPayUtil.xmlToMap(content);
            //根据文档 返回的map中的 return_code return_msg都为success，则确定请求成功，获取到code_url支付而二维码
            if("FAIL".equals(WxPayMap.get("return_code"))||"FAIL".equals((WxPayMap.get("return_msg"))))
            {
                //表示获取code-url失败
                return  R.error().message("获取code_url失败");
            }
            String codeUrl = WxPayMap.get("code_url");
            System.out.println(codeUrl);
            return  R.ok().data("code_url",codeUrl);


        } catch (Exception e) {
            return  R.error().message("未知异常312");
        }

    }

    /**
     * 用户付款后，微信服务器会向这个接口发送支付结果
     * @param request
     * @return
     * @throws IOException
     */
    @PostMapping("/notify")
    public String  getWXPayCallBackInfo(HttpServletRequest request) throws Exception {
        String ResultString = StreamUtils.inputStream2String(request.getInputStream(), "UTF-8");
        Map resultMap = WXPayUtil.xmlToMap(ResultString);//把微信支付的返回结果转成map
        boolean signatureValid = WXPayUtil.isSignatureValid(resultMap, weixinPayProperties.getPartnerKey());//判断签名
        Gson gson = new Gson();
        Map<String,String> returnWXPay=new HashMap<>();//设置接收微信支付返回信息出错时，返回给微信支付服务器的信息
        if(!signatureValid)
        {
            resultMap.put("return_code","FAIL");
            resultMap.put("return_msg","签名验证失败");
            return gson.toJson(resultMap);
        }
        if("FAIL".equals( resultMap.get("return_code"))||"FAIL".equals( resultMap.get("return_msg")))
        {
            resultMap.put("return_code","FAIL");
            resultMap.put("return_msg","返回支付结果失败");
            return gson.toJson(resultMap);
        }
        //打印微信支付服务器返回的结果
        System.out.println(resultMap);
        resultMap.put("return_code","SUCCESS");
        resultMap.put("return_msg","返回支付结果成功");
        return gson.toJson(resultMap);
    }
}
