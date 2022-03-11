package com.onegravity.knot.state

import com.badoo.reaktive.disposable.scope.DisposableScope
import com.onegravity.knot.*
import com.onegravity.knot.context.KnotContext
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import org.reduxkotlin.Store

class ReduxKnotState<State, Proposal: Any, Model: Any, ReduxModel: Any>(
    context: KnotContext,
    initialState: State,
    private val store: Store<ReduxModel>,
    selector: Selector<ReduxModel, Model>,
    mapper: Mapper<Model, State>
) : KnotState<State, Proposal>,
    DisposableScope by context.disposableScope() {

    private val state = MutableStateFlow(initialState)

    init {
        // using selectScoped will unsubscribe from the store automatically when the Knot's
        // lifecycle ends (onDestroy() called)
        store.selectScoped(this, selector) { model ->
            state.tryEmit(mapper(model))
        }
    }

    /**
     * The Stream<State>.
     */
    override val value: State = state.value

    override suspend fun collect(collector: FlowCollector<State>) {
        state.collect(collector)
    }

    /**
     * The Sink<Proposal>.
     */
    override fun emit(value: Proposal) {
        store.dispatch(value)
    }

}
