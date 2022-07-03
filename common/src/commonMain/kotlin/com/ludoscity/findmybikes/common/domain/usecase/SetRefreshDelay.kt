package com.ludoscity.findmybikes.common.domain.usecase

import com.ludoscity.findmybikes.common.data.BikeSystem
import com.ludoscity.findmybikes.common.domain.repository.FindmybikesRepositoryNew
import com.ludoscity.findmybikes.common.domain.usecase.base.BaseUseCaseInput
import com.ludoscity.findmybikes.common.domain.usecase.base.BaseUseCaseSync
import com.ludoscity.findmybikes.common.domain.usecase.base.MISSING_INPUT_EXCEPTION
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SetRefreshDelay: BaseUseCaseSync<SetRefreshDelayInput, Unit>(), KoinComponent {

    private val findmybikesRepositoryNew: FindmybikesRepositoryNew by inject()

    override fun run(): Result<Unit> {
        input?.let {
            return Result.success(findmybikesRepositoryNew.setRefreshDelay(it.newDelayMs))
        }

        throw MISSING_INPUT_EXCEPTION
    }
}

class SetRefreshDelayInput(val newDelayMs:Long): BaseUseCaseInput {

    override fun isValid(): Boolean {
        return true
    }

}