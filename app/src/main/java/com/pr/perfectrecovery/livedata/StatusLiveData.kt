package com.pr.perfectrecovery.livedata

import androidx.lifecycle.MutableLiveData
import com.pr.perfectrecovery.bean.BaseDataDTO

object StatusLiveData : MutableLiveData<BaseDataDTO>() {
    var data = SingleLiveEvent<ArrayList<BaseDataDTO>>()
    var dataSingle = SingleLiveEvent<BaseDataDTO>()
}