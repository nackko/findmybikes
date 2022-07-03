package com.ludoscity.findmybikes.common.domain.usecase.base

interface BaseUseCaseInput {
    fun isValid(): Boolean
}

const val INPUT_VALIDATION_FAIL_MSG = "Invalid use case input data"

abstract class BaseUseCaseSync <R : BaseUseCaseInput, out T : Any?> {

    protected var input: R? = null

    //@JvmName("execute")
    operator fun invoke(input: R? =null): Result<T> {
        this.input = input

        val isInputValid = input?.isValid() ?: true

        if (isInputValid) return run()

        return Result.failure(IllegalArgumentException(INPUT_VALIDATION_FAIL_MSG))
    }

    protected abstract fun run(): Result<T>
}

abstract class BaseUseCaseAsync <R : BaseUseCaseInput, out T : Any?> {

    protected var input: R? = null

    //@JvmName("execute")
    suspend operator fun invoke(input: R? =null): Result<T> {
        this.input = input

        val isInputValid = input?.isValid() ?: true

        if (isInputValid) return run()

        return Result.failure(IllegalArgumentException(INPUT_VALIDATION_FAIL_MSG))
    }

    protected abstract suspend fun run(): Result<T>
}

val MISSING_INPUT_EXCEPTION = IllegalArgumentException("Input cannot be null")