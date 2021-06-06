package com.apphud.fluttersdk.handlers

import android.app.Activity
import com.apphud.sdk.Apphud
import io.flutter.plugin.common.MethodChannel
import android.util.Log


class MakePurchaseHandler(override val routes: List<String>, val activity: Activity) : Handler {

    override fun tryToHandle(method: String, args: Map<String, Any>?, result: MethodChannel.Result) {
        when (method) {
            MakePurchaseRoutes.didFetchProductsNotification.name -> result.notImplemented()
            MakePurchaseRoutes.productsDidFetchCallback.name -> productsDidFetchCallback(result)
            MakePurchaseRoutes.refreshStoreKitProducts.name -> result.notImplemented()
            MakePurchaseRoutes.products.name -> products(result)
            MakePurchaseRoutes.product.name -> ProductParser(result).parse(args) { productIdentifier ->
                product(productIdentifier, result)
            }
            MakePurchaseRoutes.purchase.name -> PurchaseParser(result).parse(args) { productId ->
                purchase(productId, result)
            }
            MakePurchaseRoutes.purchaseWithoutValidation.name -> result.notImplemented()
            MakePurchaseRoutes.purchasePromo.name -> result.notImplemented()
            MakePurchaseRoutes.syncPurchases.name -> syncPurchases(result)
            MakePurchaseRoutes.presentOfferCodeRedemptionSheet.name -> result.notImplemented()
        }
    }

    private fun didFetchProductsNotification(result: MethodChannel.Result) {
        // not implemented
    }

    private fun productsDidFetchCallback(result: MethodChannel.Result) {
        Apphud.productsFetchCallback { skuProducts ->
            val jsonList: List<HashMap<String, Any?>> = skuProducts.map {
                DataTransformer.skuDetails(it)
            }
            result.success(jsonList)
        }
    }

    private fun products(result: MethodChannel.Result) {
        val skuProducts = Apphud.products()
        if (skuProducts != null) {
            val jsonList: List<HashMap<String, Any?>> = skuProducts.map {
                DataTransformer.skuDetails(it)
            }
            result.success(jsonList)
        } else {
            result.success(null)
        }
    }

    private fun product(productIdentifier: String, result: MethodChannel.Result) {
        val skuDetails = Apphud.product(productIdentifier = productIdentifier)
        if (skuDetails != null) {
            result.success(DataTransformer.skuDetails(skuDetails))
        } else {
            result.success(null)
        }
    }

    private fun purchase(productId: String, result: MethodChannel.Result) {
        Log.d("apphud_flutter", "purchase() was called")
        Apphud.purchase(activity, productId) { purchaseResult ->
            Log.d("apphud_flutter", "purchase() result:")

            val resultMap = hashMapOf<String, Any?>()

            purchaseResult.subscription?.let {
                resultMap["subscription"] = DataTransformer.subscription(it)
            }

            purchaseResult.nonRenewingPurchase?.let {
                resultMap["nonRenewingPurchase"] = DataTransformer.nonRenewingPurchase(it)
            }

            purchaseResult.purchase?.let {
                resultMap["purchase"] = DataTransformer.purchase(it)
            }

            purchaseResult.error?.let {
                resultMap["error"] = DataTransformer.apphudError(it)
            }

            result.success(resultMap)
        }
    }

    private fun purchaseWithoutValidation() {
        // not implemented
    }

    private fun purchasePromo() {
        // not implemented
    }

    private fun syncPurchases(result: MethodChannel.Result) {
        Apphud.syncPurchases()
        result.success(null)
    }

    class ProductParser(val result: MethodChannel.Result) {
        fun parse(args: Map<String, Any>?, callback: (productIdentifier: String) -> Unit) {
            try {
                args ?: throw IllegalArgumentException("productIdentifier is required argument")
                val productIdentifier = args["productIdentifier"] as? String
                        ?: throw IllegalArgumentException("productIdentifier is required argument")

                callback(productIdentifier)
            } catch (e: IllegalArgumentException) {
                result.error("400", e.message, "")
            }
        }
    }

    class PurchaseParser(val result: MethodChannel.Result) {
        fun parse(args: Map<String, Any>?, callback: (productId: String) -> Unit) {
            try {
                args ?: throw IllegalArgumentException("productId is required argument")
                val productId = args["productId"] as? String
                        ?: throw IllegalArgumentException("productId is required argument")

                callback(productId)
            } catch (e: IllegalArgumentException) {
                result.error("400", e.message, "")
            }
        }
    }
}

enum class MakePurchaseRoutes {
    didFetchProductsNotification,
    productsDidFetchCallback,
    refreshStoreKitProducts,
    products,
    product,
    purchase,
    purchaseWithoutValidation,
    purchasePromo,
    syncPurchases,
    presentOfferCodeRedemptionSheet;

    companion object Mapper {
        fun stringValues(): List<String> {
            return values().map { route -> route.toString() }
        }
    }
}
