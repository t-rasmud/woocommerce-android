package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.GroupedProductListAdapter.ProductViewHolder
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.OnBackorder
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import kotlinx.android.synthetic.main.grouped_product_list_item.view.*
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils

class GroupedProductListAdapter(
    private val context: Context,
    private val loadMoreListener: OnLoadMoreListener
) : RecyclerView.Adapter<ProductViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private val productList = ArrayList<Product>()
    private val bullet = "\u2022"

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productList[position].remoteId

    override fun getItemCount() = productList.size

    private fun getProductStockStatusText(product: Product): String? {
        val statusHtml = product.status?.toLocalizedString(context)
        val stock = when (product.stockStatus) {
            InStock -> {
                if (product.type == VARIABLE) {
                    if (product.numVariations > 0) {
                        context.getString(
                                R.string.product_stock_status_instock_with_variations,
                                product.numVariations
                        )
                    } else {
                        context.getString(InStock.stringResource)
                    }
                } else {
                    if (product.stockQuantity > 0) {
                        context.getString(
                                R.string.product_stock_count,
                                FormatUtils.formatInt(product.stockQuantity)
                        )
                    } else {
                        context.getString(InStock.stringResource)
                    }
                }
            }
            OutOfStock -> {
                context.getString(OutOfStock.stringResource)
            }
            OnBackorder -> {
                context.getString(OnBackorder.stringResource)
            }
            else -> {
                product.stockStatus.value
            }
        }

        return if (statusHtml != null) "$statusHtml $bullet $stock" else stock
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val holder = ProductViewHolder(LayoutInflater.from(context).inflate(R.layout.grouped_product_list_item, parent, false))
        holder.imgProduct.clipToOutline = true
        return holder
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.txtProductName.text = if (product.name.isEmpty()) {
            context.getString(R.string.untitled)
        } else {
            HtmlUtils.fastStripHtml(product.name)
        }

        val stockAndStatus = getProductStockStatusText(product)
        if (stockAndStatus != null) {
            holder.txtProductStockAndStatus.visibility = View.VISIBLE
            holder.txtProductStockAndStatus.text = HtmlCompat.fromHtml(
                    stockAndStatus,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            holder.txtProductStockAndStatus.visibility = View.GONE
        }

        val firstImage = product.firstImageUrl
        val size: Int
        if (firstImage.isNullOrEmpty()) {
            size = imageSize / 2
            holder.imgProduct.setImageResource(R.drawable.ic_product)
        } else {
            size = imageSize
            val imageUrl = PhotonUtils.getPhotonImageUrl(firstImage, imageSize, imageSize)
            GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(holder.imgProduct)
        }
        holder.imgProduct.layoutParams.apply {
            height = size
            width = size
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    fun setProductList(products: List<Product>) {
        fun isSameList(): Boolean {
            if (products.size != productList.size) {
                return false
            }
            for (index in products.indices) {
                val oldItem = productList[index]
                val newItem = products[index]
                if (!oldItem.isSameProduct(newItem)) {
                    return false
                }
            }
            return true
        }

        if (!isSameList()) {
            val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productList, products))
            productList.clear()
            productList.addAll(products)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.productImage
        val txtProductName: TextView = view.productName
        val txtProductStockAndStatus: TextView = view.productStockAndStatus
    }

    private class ProductItemDiffUtil(val items: List<Product>, val result: List<Product>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].remoteId == result[newItemPosition].remoteId

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.isSameProduct(newItem)
        }
    }
}
