package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class ShippingLabel(
    val id: Long,
    val trackingNumber: String = "",
    val carrierId: String,
    val serviceName: String,
    val status: String,
    val createdDate: Date?,
    val packageName: String,
    val rate: BigDecimal = BigDecimal.ZERO,
    val refundableAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String,
    val paperSize: String,
    val productNames: List<String>,
    val originAddress: Address? = null,
    val destinationAddress: Address? = null,
    val refund: Refund? = null
) : Parcelable {
    @IgnoredOnParcel
    var trackingLink: String? = null

    @Parcelize
    data class Refund(
        val status: String,
        val refundDate: Date?
    ) : Parcelable
}

fun WCShippingLabelModel.toAppModel(): ShippingLabel {
    return ShippingLabel(
        remoteShippingLabelId,
        trackingNumber,
        carrierId,
        serviceName,
        status,
        Date(dateCreated.toLong()),
        packageName,
        rate.toBigDecimal(),
        refundableAmount.toBigDecimal(),
        currency,
        paperSize,
        getProductNames().map { it.trim() },
        getOriginAddress()?.toAppModel(),
        getDestinationAddress()?.toAppModel(),
        getRefund()?.toAppModel()
    )
}

fun WCShippingLabelModel.ShippingLabelAddress.toAppModel(): Address {
    return Address(
        company = company ?: "",
        firstName = name ?: "",
        lastName = "",
        phone = phone ?: "",
        country = country ?: "",
        state = state ?: "",
        address1 = address ?: "",
        address2 = address2 ?: "",
        city = city ?: "",
        postcode = postcode ?: "",
        email = ""
    )
}

fun WCShippingLabelModel.WCShippingLabelRefundModel.toAppModel(): ShippingLabel.Refund {
    return ShippingLabel.Refund(
        status ?: "",
        requestDate?.let { Date(it) }
    )
}

/**
 * Method provides a list of [Order.Item] for the given [ShippingLabel.productNames]
 * in a shipping label.
 *
 * Used to display the list of products associated with a shipping label
 */
fun ShippingLabel.loadProductItems(orderItems: List<Order.Item>) =
    orderItems.filter { it.name in productNames }

/**
 * Method matches the tracking link from the [WCOrderShipmentTrackingModel] to the
 * corresponding tracking number of a [ShippingLabel]
 */
fun ShippingLabel.fetchTrackingLinks(
    orderShipmentTrackings: List<WCOrderShipmentTrackingModel>
) {
    orderShipmentTrackings.forEach { shipmentTracking ->
        if (shipmentTracking.trackingNumber == this.trackingNumber) {
            this.trackingLink = shipmentTracking.trackingLink
        }
    }
}
