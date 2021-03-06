package com.lantu.andorid.mvp_wml.injector.components;

import com.lantu.andorid.mvp_wml.injector.PerFragment;
import com.lantu.andorid.mvp_wml.injector.modules.ProductFragmentModule;
import com.lantu.andorid.mvp_wml.ui.home.information.InformationFragment;
import com.lantu.andorid.mvp_wml.ui.home.product.ProductFragment;

import dagger.Component;

/**
 * Created by wml on 2017/12/7.
 */
@PerFragment
@Component(dependencies = ApplicationComponent.class, modules = ProductFragmentModule.class)
public interface ProductFragmentComponment {
    void inject(ProductFragment fragment);
}
