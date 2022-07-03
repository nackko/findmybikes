package com.ludoscity.findmybikes.common.domain.usecase

import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.domain.repository.FindmybikesRepositoryNew
import com.ludoscity.findmybikes.common.domain.usecase.base.BaseUseCaseSync
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GetBikeSystemList: BaseUseCaseSync<Nothing, Flow<List<BikeSystem>>>(), KoinComponent {

    // todo: change name when old repo implementation will be removed from the project. Just in case
    private val findmybikesRepositoryNew: FindmybikesRepositoryNew by inject()

    override fun run(): Result<Flow<List<BikeSystem>>> {
        return Result.success(findmybikesRepositoryNew.bikeSystemList)
    }
}