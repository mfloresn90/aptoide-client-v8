package cm.aptoide.pt.billing.transaction;

import rx.Single;

public interface TransactionService {

  Single<Transaction> getTransaction(String customerId, String productId);

  Single<Transaction> createTransaction(String customerId, String productId, String serviceId,
      String payload);

  Single<Transaction> createTransaction(String customerId, String productId, String serviceId,
      String payload, String token);
}
