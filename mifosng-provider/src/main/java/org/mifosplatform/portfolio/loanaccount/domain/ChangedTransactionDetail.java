package org.mifosplatform.portfolio.loanaccount.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores details of {@link LoanTransaction}'s that were reversed or newly
 * created
 * 
 * 
 */
public class ChangedTransactionDetail {

    private List<LoanTransaction> newTransactions = new ArrayList<LoanTransaction>();

    private List<LoanTransaction> reversedTransactions = new ArrayList<LoanTransaction>();

    public List<LoanTransaction> getNewTransactions() {
        return this.newTransactions;
    }

    public List<LoanTransaction> getReversedTransactions() {
        return this.reversedTransactions;
    }

}
