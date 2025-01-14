package com.onegravity.bloc.state

import com.onegravity.bloc.utils.Acceptor
import com.onegravity.bloc.utils.logger
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

open class BlocStateImpl<State, Proposal>(
    initialState: State,
    private val acceptor: Acceptor<Proposal, State>,
) : BlocState<State, Proposal> {

    private val state = MutableStateFlow(initialState)

    /**
     * The Stream<Model>.
     */
    override val value: State
        get() = state.value

    override suspend fun collect(collector: FlowCollector<State>) {
        state.collect(collector)
    }

    /**
     * The Sink<Proposal>.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun emit(proposal: Proposal) {
        logger.d("BlocState proposal: $proposal")
        val newState = acceptor(proposal, value)
        state.tryEmit(newState)
    }
}
