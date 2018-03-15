/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 11/08/2016.
 */

package cm.aptoide.pt.billing.networking;

import android.content.SharedPreferences;
import cm.aptoide.pt.billing.BillingIdManager;
import cm.aptoide.pt.billing.BillingService;
import cm.aptoide.pt.billing.Merchant;
import cm.aptoide.pt.billing.exception.MerchantNotFoundException;
import cm.aptoide.pt.billing.exception.ProductNotFoundException;
import cm.aptoide.pt.billing.exception.PurchaseNotFoundException;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.purchase.PurchaseFactory;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v7.V7;
import cm.aptoide.pt.dataprovider.ws.v7.billing.DeletePurchaseRequest;
import cm.aptoide.pt.dataprovider.ws.v7.billing.GetMerchantRequest;
import cm.aptoide.pt.dataprovider.ws.v7.billing.GetProductsRequest;
import cm.aptoide.pt.dataprovider.ws.v7.billing.GetPurchaseRequest;
import cm.aptoide.pt.dataprovider.ws.v7.billing.GetPurchasesRequest;
import cm.aptoide.pt.dataprovider.ws.v7.billing.GetServicesRequest;
import cm.aptoide.pt.install.PackageRepository;
import java.util.Collections;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Single;

public class BillingServiceV7 implements BillingService {

  private final OkHttpClient httpClient;
  private final Converter.Factory converterFactory;
  private final TokenInvalidator tokenInvalidator;
  private final SharedPreferences sharedPreferences;
  private final PurchaseMapperV7 purchaseMapper;
  private final ProductMapperV7 productMapperV7;
  private final PackageRepository packageRepository;
  private final PaymentServiceMapper serviceMapper;
  private final BodyInterceptor<BaseBody> bodyInterceptorV7;
  private final BillingIdManager billingIdManager;
  private final PurchaseFactory purchaseFactory;

  public BillingServiceV7(BodyInterceptor<BaseBody> bodyInterceptorV7, OkHttpClient httpClient,
      Converter.Factory converterFactory, TokenInvalidator tokenInvalidator,
      SharedPreferences sharedPreferences, PurchaseMapperV7 purchaseMapper,
      ProductMapperV7 productMapperV7, PackageRepository packageRepository,
      PaymentServiceMapper serviceMapper, BillingIdManager billingIdManager,
      PurchaseFactory purchaseFactory) {
    this.httpClient = httpClient;
    this.converterFactory = converterFactory;
    this.tokenInvalidator = tokenInvalidator;
    this.sharedPreferences = sharedPreferences;
    this.purchaseMapper = purchaseMapper;
    this.productMapperV7 = productMapperV7;
    this.packageRepository = packageRepository;
    this.serviceMapper = serviceMapper;
    this.bodyInterceptorV7 = bodyInterceptorV7;
    this.billingIdManager = billingIdManager;
    this.purchaseFactory = purchaseFactory;
  }

  @Override public Single<List<PaymentService>> getPaymentServices() {
    return GetServicesRequest.of(sharedPreferences, httpClient, converterFactory, bodyInterceptorV7,
        tokenInvalidator)
        .observe(false)
        .toSingle()
        .flatMap(response -> {
          if (response != null && response.isOk()) {
            return Single.just(serviceMapper.map(response.getList()));
          } else {
            return Single.error(new IllegalStateException(V7.getErrorMessage(response)));
          }
        });
  }

  @Override public Single<Merchant> getMerchant(String merchantName) {
    return GetMerchantRequest.of(merchantName, bodyInterceptorV7, httpClient, converterFactory,
        tokenInvalidator, sharedPreferences)
        .observe(false)
        .toSingle()
        .flatMap(response -> {
          if (response != null && response.isOk()) {
            return Single.just(new Merchant(response.getData()
                .getId(), response.getData()
                .getName()));
          } else {
            return Single.error(new MerchantNotFoundException(V7.getErrorMessage(response)));
          }
        });
  }

  @Override public Completable deletePurchase(String purchaseId) {
    return DeletePurchaseRequest.of(billingIdManager.resolvePurchaseId(purchaseId), httpClient,
        converterFactory, bodyInterceptorV7, tokenInvalidator, sharedPreferences)
        .observe(true)
        .first()
        .toSingle()
        .flatMapCompletable(response -> {
          if (response != null && response.isOk()) {
            return Completable.complete();
          }
          return Completable.error(new PurchaseNotFoundException(V7.getErrorMessage(response)));
        });
  }

  @Override public Single<List<Purchase>> getPurchases(String merchantName) {
    return GetPurchasesRequest.of(merchantName, bodyInterceptorV7, httpClient, converterFactory,
        tokenInvalidator, sharedPreferences)
        .observe(true)
        .toSingle()
        .flatMap(response -> {

          if (response.isSuccessful()) {
            return Single.just(purchaseMapper.map(response.body()
                .getList()));
          }

          // If user not logged in return a empty purchase list.
          return Single.<List<Purchase>>just(Collections.emptyList());
        });
  }

  @Override public Single<Purchase> getPurchase(String productId) {
    return GetPurchaseRequest.of(billingIdManager.resolveProductId(productId), bodyInterceptorV7,
        httpClient, converterFactory, tokenInvalidator, sharedPreferences)
        .observe(true)
        .toSingle()
        .flatMap(response -> {

          if (response.isSuccessful()) {
            return Single.just(purchaseMapper.map(response.body()
                .getData()));
          }

          if (response.code() == 404) {
            return Single.just(
                purchaseFactory.create(productId, null, null, Purchase.Status.NEW, null, null, null,
                    null));
          }

          return Single.just(
              purchaseFactory.create(productId, null, null, Purchase.Status.FAILED, null, null,
                  null, null));
        });
  }

  @Override public Single<List<Product>> getProducts(String merchantName, List<String> skus) {
    return GetProductsRequest.of(merchantName, skus, bodyInterceptorV7, httpClient,
        converterFactory, tokenInvalidator, sharedPreferences)
        .observe(false)
        .first()
        .toSingle()
        .flatMap(response -> {
          if (response != null && response.isOk()) {
            return mapToProducts(merchantName, response.getList());
          } else {
            return Single.<List<Product>>error(
                new IllegalStateException(V7.getErrorMessage(response)));
          }
        });
  }

  @Override public Single<Product> getProduct(String sku, String merchantName) {
    return GetProductsRequest.of(merchantName, sku, bodyInterceptorV7, httpClient, converterFactory,
        tokenInvalidator, sharedPreferences)
        .observe(false)
        .first()
        .toSingle()
        .flatMap(response -> {
          if (response != null && response.isOk()) {
            return mapToProduct(merchantName, response.getData());
          } else {
            return Single.error(new ProductNotFoundException("No product found for sku: " + sku));
          }
        });
  }

  private Single<List<Product>> mapToProducts(String merchantName,
      List<GetProductsRequest.ResponseBody.Product> responseList) {
    return Single.zip(packageRepository.getPackageVersionCode(merchantName),
        packageRepository.getPackageLabel(merchantName),
        (packageVersionCode, applicationName) -> productMapperV7.map(merchantName, responseList,
            packageVersionCode));
  }

  private Single<Product> mapToProduct(String merchantName,
      GetProductsRequest.ResponseBody.Product response) {
    return Single.zip(packageRepository.getPackageVersionCode(merchantName),
        packageRepository.getPackageLabel(merchantName),
        (packageVersionCode, applicationName) -> productMapperV7.map(merchantName,
            packageVersionCode, response));
  }
}
