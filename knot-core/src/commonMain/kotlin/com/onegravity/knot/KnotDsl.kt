package com.onegravity.knot

import com.onegravity.knot.builder.FullKnotBuilder
import com.onegravity.knot.builder.FullKnotBuilderImpl
import com.onegravity.knot.builder.SimpleKnotBuilder
import com.onegravity.knot.builder.SimpleKnotBuilderImpl
import com.onegravity.knot.context.KnotContext
import com.onegravity.knot.state.KnotState
import com.onegravity.knot.state.knotState
import kotlin.jvm.JvmName

/**
 * Creates a [Knot] instance using a [FullKnotBuilder].
 */
@JvmName("fullKnot")
fun <State, Event, Proposal, SideEffect> knot(
    context: KnotContext,
    knotState: KnotState<State, Proposal>,
    block: FullKnotBuilder<State, Event, Proposal, SideEffect>.() -> Unit
): Knot<State, Event, Proposal, SideEffect> =
    FullKnotBuilderImpl<State, Event, Proposal, SideEffect>()
    .also(block)
    .build(context, knotState)

/**
 * Creates a [Knot] instance using a [SimpleKnotBuilder]
 */
@JvmName("simplerKnot")
fun <State, Event> knot(
    context: KnotContext,
    knotState: KnotState<State, State>,
    block: SimpleKnotBuilder<State, Event>.() -> Unit
): Knot<State, Event, State, SideEffect<Event>> =
    SimpleKnotBuilderImpl<State, Event>()
        .also(block)
        .build(context, knotState)

/**
 * Creates a [Knot] instance using a [SimpleKnotBuilder].
 */
@JvmName("simplestKnot")
fun <State, Event> knot(
    context: KnotContext,
    initialState: State,
    block: SimpleKnotBuilder<State, Event>.() -> Unit
): Knot<State, Event, State, SideEffect<Event>> =
    SimpleKnotBuilderImpl<State, Event>()
    .also(block)
    .build(context, knotState(initialState))
