package com.silencefly96.module_demo.plan

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.silencefly96.module_base.base.BaseActivity
import com.silencefly96.module_demo.databinding.ActivityPlanBinding
import com.silencefly96.module_demo.plan.model.Injection
import com.silencefly96.module_common.ext.replaceFragment
import com.silencefly96.module_demo.databinding.ActivityMainBinding

class PlanActivity : BaseActivity() {

    private lateinit var binding: ActivityPlanBinding

    private lateinit var planViewModel: PlanViewModel

    override fun bindView(): View {
        binding = ActivityPlanBinding.inflate(layoutInflater)
        //禁用沉浸状态栏
        isSteepStatusBar = false
        return binding.root
    }

    override fun doBusiness(context: Context) {
        //viewmodel和activity生命周期不一致，通过ViewModelProvider提供
        planViewModel = ViewModelProvider(this).get(PlanViewModel::class.java)
        //通过注入提供Repository，便于测试
        planViewModel.planRepository = Injection.providePlanRepository(this)

        //fragment才是真正的view层
        replaceFragment(PlanListFragment.newInstance(planViewModel), binding.listFrame.id)
        //PlanDetailFragment通过侧边栏提供
        replaceFragment(PlanDetailFragment.newInstance(planViewModel), binding.detailFrame.id)
        //TestFragment可以控制所有数据
        replaceFragment(PlanTestFragment.newInstance(planViewModel), binding.testFrame.id)
    }
}