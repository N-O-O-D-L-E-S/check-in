package org.usc.check.in.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.usc.check.in.model.Account;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 *
 * @author Shunli
 */
@Component
public class ZiMuZuTvSignInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(ZiMuZuTvSignInTask.class);

    private static final String URL = "http://www.zimuzu.tv/";
    private static final String LOGIN_URL = "http://www.zimuzu.tv/User/Login/ajaxLogin";
    private static final String SIGN_IN_URL = "http://www.zimuzu.tv/user/sign";

    @Override
    protected String name() {
        return "zimuzu";
    }

    @Scheduled(cron = "0 0 7,18 * * ?")
    public void run() {
        for (Account account : buildAccounts()) {
            try {
                Executor executor = Executor.newInstance().use(new BasicCookieStore());
                if (login(executor, account)) {
                    signIn(executor, account);
                }
            } catch (Exception e) {
                log.error("【ZIMUZU】【" + account.getUsername() + "】签到异常", e);
            }
        }
    }

    private boolean login(Executor executor, Account account) throws ClientProtocolException, IOException {
        String usrename = account.getUsername();

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("account", usrename));
        formParams.add(new BasicNameValuePair("password", account.getPassword()));
        formParams.add(new BasicNameValuePair("remember", "0"));
        formParams.add(new BasicNameValuePair("url_back", URL));

        String loginJson = executor.execute(appendTimeOuts(Request.Post(LOGIN_URL)).bodyForm(formParams)).returnContent().asString();
        JSONObject loginJsonParseObject = JSON.parseObject(loginJson);
        if (1 != loginJsonParseObject.getInteger("status")) {
            log.info("【ZIMUZU】【{}】登录失败：{}", usrename, loginJsonParseObject.getString("info"));
            return false;
        }

        log.info("【ZIMUZU】【{}】登录成功", usrename);
        return true;
    }

    private boolean signIn(Executor executor, Account account) throws ClientProtocolException, IOException, InterruptedException {
        String usrename = account.getUsername();

        executor.execute(appendTimeOuts(Request.Get(URL))).discardContent();
        executor.execute(appendTimeOuts(Request.Get("http://www.zimuzu.tv/public/hotkeyword"))).discardContent();
        executor.execute(appendTimeOuts(Request.Get("http://www.zimuzu.tv/user/login/getCurUserTopInfo"))).discardContent();

        String rtn = executor.execute(appendTimeOuts(Request.Get(SIGN_IN_URL))).returnContent().asString();
        if (StringUtils.contains(rtn, "您今天已登录")) {
            log.info("【ZIMUZU】【{}】签到成功", usrename);
            return true;
        }

        log.info("【ZIMUZU】【{}】签到失败", usrename);
        return false;
    }

}
