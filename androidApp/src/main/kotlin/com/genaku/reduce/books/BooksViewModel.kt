package com.genaku.reduce.books

import com.genaku.reduce.BaseViewModel
import com.onegravity.knot.ActivityKnotContext
import com.onegravity.knot.sample.books.*

class BooksViewModel(context: ActivityKnotContext) : BaseViewModel(context), BooksUseCase {

//    private val useCase: BooksUseCase = BooksUseCaseImplRedux(viewModelContext)
//    private val useCase: BooksUseCase = BooksUseCaseImpl(viewModelContext, BooksRepositoryImpl())
    private val useCase: BooksUseCase = BooksUseCaseImplSimple(viewModelContext, BooksRepositoryImpl())

    override val state
        get() = useCase.state

    override fun load() {
        useCase.load()
    }

    override fun clear() {
        useCase.clear()
    }
}