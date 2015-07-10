package org.usc.check.in.task;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.usc.check.in.model.Account;

/**
 *
 * @author Shunli
 */
public abstract class BaseTask {
    @Autowired
    private XMLConfiguration config;

    protected abstract String name();

    protected List<Account> buildAccounts() {
        List<Account> accounts = new ArrayList<Account>();

        List<HierarchicalConfiguration> configurationsAt = config.configurationsAt(name() + ".accounts.account");
        for (HierarchicalConfiguration hierarchicalConfiguration : configurationsAt) {
            String username = hierarchicalConfiguration.getString("username");
            String password = hierarchicalConfiguration.getString("password");

            accounts.add(new Account(username, password));
        }

        return accounts;
    }
}