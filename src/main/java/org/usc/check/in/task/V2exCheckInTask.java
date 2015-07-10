package org.usc.check.in.task;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.usc.check.in.model.Account;

/**
 *
 * @author Shunli
 */
public class V2exCheckInTask extends BaseTask {
    private static final Logger log = LoggerFactory.getLogger(V2exCheckInTask.class);

    private static final String LOGIN_URL = "http://www.v2ex.com/signin";
    private static final String CHECK_IN_URL = "http://www.v2ex.com/mission/daily";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36";

    @Override
    protected String name() {
        return "v2ex";
    }

    @Scheduled(cron = "0 0 9,18 * * ?")
    public void run() {
        for (Account account : buildAccounts()) {
            try {
                Executor executor = Executor.newInstance().cookieStore(new BasicCookieStore());
                if (login(executor, account)) {
                    checkIn(executor, account);
                }
            } catch (Exception e) {
                log.error("【V2EX】【" + account.getUsername() + "】签到异常", e);
            }

        }
    }

    private boolean login(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException {
        String usrename = account.getUsername();

        // 1st get once
        Document checkLoginOnce = Jsoup.parse(executor.execute(Request.Get(LOGIN_URL)).returnContent().asString());
        String once = checkLoginOnce.getElementsByAttributeValue("name", "once").attr("value");

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("u", usrename));
        formParams.add(new BasicNameValuePair("p", account.getPassword()));
        formParams.add(new BasicNameValuePair("once", once));
        formParams.add(new BasicNameValuePair("next", "/"));

        // login
        executor.execute(Request.Post(LOGIN_URL).bodyForm(formParams).userAgent(USER_AGENT).addHeader("Referer", "http://www/v2ex.com/signin"));

        // checkIn must load first page once
        String rtn = executor.execute(Request.Get("http://www.v2ex.com")).returnContent().asString();
        if (StringUtils.contains(rtn, "signout")) {
            log.info("【V2EX】【{}】登录成功", usrename);
            return true;
        }

        log.info("【V2EX】【{}】登录失败", usrename);
        return false;
    }

    private boolean checkIn(Executor executor, Account account) throws ClientProtocolException, IOException, URISyntaxException, InterruptedException {
        String usrename = account.getUsername();

        String rtn = executor.execute(Request.Get(CHECK_IN_URL)).returnContent().asString();
        if (StringUtils.contains(rtn, "fa-ok-sign")) {
            log.info("【V2EX】【{}】每日登录奖励已领取，当前账户余额：{}", usrename, getBalance(rtn));
            return true;
        }

        Elements element = Jsoup.parse(rtn).getElementsByAttributeValueMatching("onclick", "/mission/daily/redeem");
        String once = StringUtils.substringBetween(element.attr("onclick"), "'", "'");
        if (StringUtils.isNotEmpty(once)) {
            String url = "http://www.v2ex.com" + once;

            String checkInRtn = executor.execute(Request.Get(url).userAgent(USER_AGENT).addHeader("Referer", CHECK_IN_URL)).returnContent().asString();
            log.info("【V2EX】【{}】签到成功，当前账户余额：{}", usrename, getBalance(checkInRtn));
            return true;
        }

        log.info("【V2EX】【{}】签到成功", usrename);
        return false;
    }

    private String getBalance(String rtn) {
        return Jsoup.parse(rtn).getElementsByClass("balance_area").text();
    }

}
