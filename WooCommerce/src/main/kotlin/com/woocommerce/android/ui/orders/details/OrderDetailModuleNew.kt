package com.woocommerce.android.ui.orders.details

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class OrderDetailModuleNew {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: OrderDetailFragmentNew): Bundle? {
            return fragment.arguments
        }
    }

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: OrderDetailFragmentNew): SavedStateRegistryOwner

    @Binds
    @IntoMap
    @ViewModelKey(OrderDetailViewModelNew::class)
    abstract fun bindFactory(factory: OrderDetailViewModelNew.Factory): ViewModelAssistedFactory<out ViewModel>
}
