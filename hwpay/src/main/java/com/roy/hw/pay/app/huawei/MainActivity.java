package com.roy.hw.pay.app.huawei;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.support.api.client.Status;
import com.roy.hw.pay.app.R;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "roy_hwpay";
    private static String productId = "test.HKD.001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    /**
     * Checking the Support for HUAWEI IAP
     *
     * @param activity The Activity instance that calls the isEnvReady API.
     * @throws NullPointerException If the activity is null.
     */
    private void queryIsReady(Activity activity) {
        Task<IsEnvReadyResult> task = Iap.getIapClient(activity).isEnvReady();
        task.addOnSuccessListener(new OnSuccessListener<IsEnvReadyResult>() {
            @Override
            public void onSuccess(IsEnvReadyResult result) {
                // Obtain the execution result.
                showAndLog("init success "+result.getReturnCode());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                // Handle the exception.
                showAndLog("init onFailure "+e.getMessage());
            }
        });
    }


    /**
     * Presenting Product Information
     *
     * @param productIdList The product ID list of products to be queried.
     *                      The product ID is the same as that set by a developer when configuring product information in AppGallery Connect.
     *                      A maximum of 200 product IDs can be obtained at a time.
     *                      You can refer to the following link to configure product information in AppGallery Connect
     *                      https://developer.huawei.com/consumer/en/doc/distribution/app/agc-create_product#h1-1574820228194
     * @param activity      The Activity instance that calls the obtainProductInfo API.
     * @throws NullPointerException If the activity is null.
     */
    private void queryProducts(List<String> productIdList, Activity activity) {
        ProductInfoReq req = new ProductInfoReq();
        // priceType: 0: consumable; 1: non-consumable; 2: auto-renewable subscription
        req.setPriceType(IapClient.PriceType.IN_APP_CONSUMABLE);
        req.setProductIds(productIdList);
        // to call the obtainProductInfo API
        Task<ProductInfoResult> task = Iap.getIapClient(activity).obtainProductInfo(req);
        task.addOnSuccessListener(new OnSuccessListener<ProductInfoResult>() {
            @Override
            public void onSuccess(ProductInfoResult result) {
                // Obtain the result
                List<ProductInfo> productList = result.getProductInfoList();
                showAndLog("queryProducts success "+" size "+productIdList.size()+" :"+productIdList.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    int returnCode = apiException.getStatusCode();
                    showAndLog("queryProducts fail :"+returnCode);
                } else {
                    // Other external errors
                    showAndLog("queryProducts fail :"+e.getMessage());
                }
            }
        });
    }

    /**
     * Initiating a Purchase
     *
     * @param productId The product ID of the product which you want to buy.
     *                  The product ID is the same as that set by a developer when configuring product information in AppGallery Connect.
     *                  The value length is within (0, 148).
     *                  The product ID must be unique within an app. Once saved, the product ID cannot be changed. This ID cannot be used again even if the product is deleted.
     *                  You can refer to the following link to configure product information in AppGallery Connect
     *                  https://developer.huawei.com/consumer/en/doc/distribution/app/agc-create_product#h1-1574820228194
     * @param activity  The Activity instance that calls the createPurchaseIntent API
     * @throws NullPointerException If the activity is null.
     */
    private void buy(String productId, final Activity activity) {
        // Constructs a PurchaseIntentReq object.
        PurchaseIntentReq req = new PurchaseIntentReq();
        req.setProductId(productId);
        // In-app product type contains:
        // priceType: 0: consumable; 1: non-consumable; 2: auto-renewable subscription
        req.setPriceType(IapClient.PriceType.IN_APP_CONSUMABLE);
        // The value length is within (0, 256).
        req.setDeveloperPayload("testPurchase");
        // to call the createPurchaseIntent API.
        Task<PurchaseIntentResult> task = Iap.getIapClient(activity).createPurchaseIntent(req);
        task.addOnSuccessListener(new OnSuccessListener<PurchaseIntentResult>() {
            @Override
            public void onSuccess(PurchaseIntentResult result) {
                // Obtain the payment result.
                Status status = result.getStatus();
                if (status.hasResolution()) {
                    try {
                        // 6666 is an int constant defined by the developer.
                        status.startResolutionForResult(activity, 6666);
                        showAndLog("buy success and has Resolution : paymentData - "+result.getPaymentData()+" pay signature - "+result.getPaymentSignature());
//                        deliverProduct(result.getPaymentData(),true,MainActivity.this);
                    } catch (IntentSender.SendIntentException exp) {
                        showAndLog("buy success and has Resolution but has error :"+exp.getMessage());
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                showAndLog("buy fail :"+e.getMessage());
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    int statusCode = apiException.getStatusCode();
                    if (statusCode == OrderStatusCode.ORDER_PRODUCT_OWNED) {
                        //Checking if there exists undelivered products.
                        showAndLog("buy fail there exists undelivered products,will deliver ");
                        queryPurchases(MainActivity.this);
                    } else {
                        // Other errors
                        showAndLog("buy fail : "+statusCode);
                    }
                } else {
                    // Other external errors
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 6666) {
            if (data == null) {
                Log.e("onActivityResult", "data is null");
                return;
            }
            PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data);
            switch (purchaseResultInfo.getReturnCode()) {
                case OrderStatusCode.ORDER_STATE_CANCEL:
                    // User cancel payment.
                    showAndLog("User cancel payment.");
                    break;
                case OrderStatusCode.ORDER_STATE_FAILED:
                case OrderStatusCode.ORDER_PRODUCT_OWNED:
                    // Checking if there exists undelivered products.
                    showAndLog("Checking if there exists undelivered products.");
                    queryPurchases(MainActivity.this);
                    break;
                case OrderStatusCode.ORDER_STATE_SUCCESS:
                    // pay success.
                    String inAppPurchaseData = purchaseResultInfo.getInAppPurchaseData();
                    String inAppPurchaseDataSignature = purchaseResultInfo.getInAppDataSignature();
                    showAndLog("pay sucess in activityResult inAppPurchaseData："+inAppPurchaseData);
                    showAndLog("pay sucess in activityResult inAppPurchaseDataSignature："+inAppPurchaseDataSignature);


                    // Delivering a Consumable Product
                    break;
                default:
                    break;
            }
            return;
        }
    }


    /**
     * Checking if there exists undelivered products.
     * Call the obtainOwnedPurchases API to obtain the purchase information about the consumables that have been purchased but not delivered.
     *
     * @param activity The Activity instance that calls the obtainOwnedPurchases API
     * @throws NullPointerException If the activity is null.
     */
    private void queryPurchases(Activity activity) {
        // Constructs a OwnedPurchasesReq object.
        OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
        // In-app product type contains:
        // priceType: 0: consumable; 1: non-consumable; 2: auto-renewable subscription
        ownedPurchasesReq.setPriceType(IapClient.PriceType.IN_APP_CONSUMABLE);
        // to call the obtainOwnedPurchases API
        Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchasesReq);
        task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
            @Override
            public void onSuccess(OwnedPurchasesResult result) {
                // Obtain the execution result.
                if (result != null && result.getInAppPurchaseDataList() != null) {
                    for (int i = 0; i < result.getInAppPurchaseDataList().size(); i++) {
                        String inAppPurchaseData = result.getInAppPurchaseDataList().get(i);
                        String InAppSignature = result.getInAppSignature().get(i);
                        // Delivering a Consumable Product
                        showAndLog(" deliver goods "+inAppPurchaseData+" signature "+InAppSignature);
                        deliverProduct(inAppPurchaseData,true,MainActivity.this);
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    Status status = apiException.getStatus();
                    int returnCode = apiException.getStatusCode();
                } else {
                    // Other external errors
                }
            }
        });
    }

    /**
     * Getting the public key of the application.
     * You can query the public key through the following steps
     * 1. Sign in to AppGallery Connect and click My projects.
     * 2. Find your app project, and click the app for which you need to query IAP information.
     * 3. Go to Earning > In-App Purchases and find the public key for your app.
     * https://developer.huawei.com/consumer/en/doc/HMSCore-Guides-V5/query-payment-info-0000001050166299-V5
     *
     * @return publicKey
     */
    public String getPublicKey() {
        return "***";
    }
    /**
     * If your app has high security requirements, further send a verification request to the
     * Huawei IAP server through the API of Verifying the Purchase Token for the Order Service on your app server.
     * Verify signature information.
     *
     * @param content   Result string.
     * @param sign      Signature string.
     * @param publicKey Payment public key.
     * @return boolean
     */
    public boolean doCheck(String content, String sign, String publicKey) {
        if (sign == null) {
            return false;
        }
        if (publicKey == null) {
            return false;
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.decode(publicKey, Base64.DEFAULT);
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
            java.security.Signature signature = java.security.Signature.getInstance("SHA256WithRSA");
            signature.initVerify(pubKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            byte[] bsign = Base64.decode(sign, Base64.DEFAULT);
            return signature.verify(bsign);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * If the signature information is legal, you can deliver your products.
     * If the user purchased a consumable product, call the consumeOwnedPurchase API to consume it after successfully delivering the product.
     *
     * @param inAppPurchaseDataStr inAppPurchaseData
     *                             The length of parameter 'developerChallenge' is within (0, 64).
     *                             The max length of parameter 'purchaseToken' is 128.
     * @param isSignatureLegal     The legality of the signature
     * @param activity             The Activity instance that calls the consumeOwnedPurchase API
     * @throws NullPointerException If the activity is null.
     */
    private void deliverProduct(final String inAppPurchaseDataStr, boolean isSignatureLegal, Activity activity) {
        if (isSignatureLegal) {
            // You can deliver your products.
            // ...
            // Call the consumeOwnedPurchase API to consume it after successfully delivering the product.
            // Constructs a ConsumeOwnedPurchaseReq object.
            String purchaseToken = "";
            try {
                InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseDataStr);
                purchaseToken = inAppPurchaseDataBean.getPurchaseToken();
            } catch (JSONException e) {
            }
            ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
            req.setPurchaseToken(purchaseToken);
            // To call the consumeOwnedPurchase API.
            Task<ConsumeOwnedPurchaseResult> task = Iap.getIapClient(activity).consumeOwnedPurchase(req);
            task.addOnSuccessListener(new OnSuccessListener<ConsumeOwnedPurchaseResult>() {
                @Override
                public void onSuccess(ConsumeOwnedPurchaseResult result) {
                    // Obtain the result

                    showAndLog("deliverProduct success  "+result.getConsumePurchaseData());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    if (e instanceof IapApiException) {
                        IapApiException apiException = (IapApiException) e;
                        Status status = apiException.getStatus();
                        int returnCode = apiException.getStatusCode();
                        showAndLog("deliverProduct fail  status : "+status+" returnCode : "+returnCode);
                    } else {
                        // Other external errors
                        showAndLog("deliverProduct fail ex : "+e.getMessage());
                    }
                }
            });
        } else {
            // Verify signature failed
            showAndLog("deliverProduct fail  : Verify signature failed");
        }
    }


    /**
     * Query the Purchase History
     *
     * @param activity The Activity instance that calls the obtainOwnedPurchaseRecord API
     * @throws NullPointerException If the activity is null.
     */
    private void queryHistory(Activity activity) {
        // Constructs a OwnedPurchasesReq object.
        final OwnedPurchasesReq req = new OwnedPurchasesReq();
        // In-app product type contains:
        // 0: consumable; 1: non-consumable; 2: auto-renewable subscription
        req.setPriceType(IapClient.PriceType.IN_APP_CONSUMABLE);
        // To call the obtainOwnedPurchaseRecord API.
        Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchaseRecord(req);
        task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
            @Override
            public void onSuccess(OwnedPurchasesResult result) {
                // Obtain the execution result.
                List<String> inAppPurchaseDataList = result.getInAppPurchaseDataList();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    int returnCode = apiException.getStatusCode();
                } else {
                    // Other external errors
                }
            }
        });
    }

    public void queryGoods(View view) {
        List<String> items=new ArrayList<>();
        items.add(productId);
        queryProducts(items,this);
    }

    public void isHwPayReady(View view) {
        queryIsReady(this);

    }

    public void buyGoods(View view) {
        buy(productId,this);
    }

    public void showAndLog(String content){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, content);
                Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT).show();
            }
        });

    }



}