/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 22/11/2016.
 */

package cm.aptoide.pt.billing.sync;

import cm.aptoide.pt.billing.Customer;
import cm.aptoide.pt.billing.authorization.AuthorizationPersistence;
import cm.aptoide.pt.billing.authorization.AuthorizationService;
import cm.aptoide.pt.billing.authorization.LocalIdGenerator;
import cm.aptoide.pt.billing.authorization.MetadataAuthorization;
import cm.aptoide.pt.sync.Sync;
import rx.Completable;

public class AuthorizationSync extends Sync {

  private final String transactionId;
  private final Customer customer;
  private final AuthorizationService authorizationService;
  private final AuthorizationPersistence authorizationPersistence;
  private final LocalIdGenerator idGenerator;

  public AuthorizationSync(String id, Customer customer, String transactionId,
      AuthorizationService authorizationService, AuthorizationPersistence authorizationPersistence,
      boolean periodic, boolean exact, long interval, long trigger, LocalIdGenerator idGenerator) {
    super(id, periodic, exact, trigger, interval);
    this.transactionId = transactionId;
    this.customer = customer;
    this.authorizationService = authorizationService;
    this.authorizationPersistence = authorizationPersistence;
    this.idGenerator = idGenerator;
  }

  @Override public Completable execute() {
    return customer.getId()
        .flatMapCompletable(
            customerId -> syncRemoteAuthorization(customerId, transactionId).onErrorComplete()
                .andThen(syncMetadataAuthorization(customerId, transactionId)));
  }

  private Completable syncMetadataAuthorization(String customerId, String transactionId) {
    return authorizationPersistence.getAuthorization(customerId, transactionId)
        .first()
        .filter(authorization -> authorization instanceof MetadataAuthorization)
        .cast(MetadataAuthorization.class)
        .filter(authorization -> authorization.isPendingSync())
        .flatMapSingle(authorization -> authorizationService.updateAuthorization(customerId,
            authorization.getTransactionId(), authorization.getMetadata()))
        .flatMapCompletable(
            authorization -> authorizationPersistence.saveAuthorization(authorization))
        .toCompletable();
  }

  private Completable syncRemoteAuthorization(String customerId, String transactionId) {

    if (idGenerator.isLocal(transactionId)) {
      return Completable.complete();
    }

    return authorizationService.getAuthorization(transactionId, customerId)
        .flatMapCompletable(
            authorization -> authorizationPersistence.saveAuthorization(authorization));
  }

  public String getTransactionId() {
    return transactionId;
  }
}