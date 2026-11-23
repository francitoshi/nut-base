# The Actor Framework Constitution

The design rules of `io.github.francitoshi.nut.base.commons.concurrent.actor`.
Every rule states an invariant the framework promises, why it exists, and the
mechanism that enforces it. Rules were mostly learned the hard way; the
"Enforced by" lines name the code that must be preserved when touching this
package.

The model in one paragraph: an `ActorHub` owns a thread pool and hosts
`Actor` stages. Each actor owns a `CloseableChannel` (its mailbox) and a set
of worker threads that drain it. Producers call `accept(m)`; workers pull.
Nothing a producer does ever runs a message, and nothing an actor does ever
blocks a producer beyond the channel's own backpressure.

---

## R1 — The producer never processes

**Rule.** `accept(m)` only enqueues. The calling thread never runs message
code — not its own message, not anybody else's.

**Rule (backpressure side).** `accept(m)` waits for mailbox room
*permanently*: there is no immediate rejection — the wait always terminates
because R2 guarantees the permanent worker keeps draining the mailbox — and
the wait is **not abortable by interruption** (the interrupt is consumed,
like every framework wait). The only rejection is a closed actor: a returned
`accept` means the message will be processed (R4). Producers waiting for
room queue up behind whatever the channel wakes first; the guarantee the
framework makes is FIFO *by enqueue order* — once a message enters the
mailbox it is served in that strict order (multi-worker actors guarantee
the start order, not the completion order, see R5).

**Why.** The obvious fear is ordering: an inline execution would let a later
message start before earlier queued ones. The dequeue is FIFO, so start order
survives today, but the real reasons are stronger:

1. **Reentrancy.** A producer running handlers inside `accept()` while
   holding its own locks self-deadlocks the moment a handler needs the same
   lock. It also processes *other producers'* messages on its stack, with
   unbounded work per call (`drain(0)` empties the whole channel).
2. **Contract clarity.** `accept()` must mean "queued for later". If it
   sometimes executes, every future change (steal-order, priorities,
   batching) can silently break user reasoning.
3. **History.** The `CallerRunsPolicy` once executed the *permanent worker*
   inline in the producer, parking that thread inside the actor's drain loop
   until close (found via thread dump). R1 removes the entire class.

**Exemption.** `SynchronousActor` is the deliberate "runs in the caller"
flavour (`threads == 0`, or a synchronous hub). It is the only exception, and
it is opt-in by construction.

**Enforced by.** The mailbox channels of the actors are built with the
mailbox flavour (`Channel.closeableMailboxOf` /
`closeableMailboxBuffered`): a producer's own interruption does not abort
its put and does not close the channel — the wait survives the interrupt
and the message is enqueued as soon as there is room. The lazy rush start
skips instead of degrading to inline work when the pool cannot admit it
(see R2/R3); the permanent worker's three-tier admission (R3) never runs
the task in the caller. The hub's pool is built with an abort-style
saturation policy, so a rejection is always an exception the framework
resolves explicitly — it can never silently become inline execution.

---

## R2 — N threads is a ceiling, not a guarantee

**Rule.** An actor declaring `N` worker threads promises at most `N`
concurrent workers and *guarantees* exactly one: its **permanent worker**,
which lives for as long as the actor exists and always drains the channel.
Every additional "rush" worker is opportunistic.

**Why.** Chasing the full `N` on every message made the arrival path pay a
double wake-up (the channel wakes the permanent worker, `execute()` wakes a
rush worker — one of the two is wasted) plus a semaphore CAS, a task
allocation and a pool hand-off per message. Measured: ×2 throughput in the
single-producer case once the rush became lazy (`counters.first > 1`, i.e.
there is a backlog worth parallelising).

**Enforced by.** `AsyncActor.accept`: the rush start only fires on backlog;
`MultiActor`/`SingleActor` share the machinery through `AsyncActor`.

---

## R3 — The permanent worker must always start, and must never capture the caller

**Rule.** The permanent worker is the actor's safety net: it must start
reliably, and neither the saturation policy nor the pool geometry may run it
in the calling thread (a `drainWhileRegistered` parks its thread until close)
or block the caller waiting for a slot (permanent workers never poll the
pool's queue, so under a fully-busy-at-maximum pool the wait has no
guaranteed end).

**Why.** Thread dumps under load showed two distinct failure shapes with the
naive design: producers killed by a saturation rejection (leaving an actor
with queued messages and no worker), and producers captured forever inside
`permanentLoop` when the rejection policy ran the task inline.

**Enforced by.** Three-tier admission in `ActorPool.executeUntracked`:
(1) pool at maximum and fully busy → the worker gets a dedicated thread built
with the pool's own `ThreadFactory`; (2) otherwise a non-blocking `offer` on
the pool's hand-off queue delivers the task without ever reaching the
rejection handler; (3) as a last resort the task is submitted wrapped in a
guard — if the rejection policy runs the wrapper in the submitting thread
(what `CallerRunsPolicy` does), the guard hands the real task to a dedicated
thread instead. With `CallerWaitsPolicy` the guard cannot help (the policy
blocks inside its own `rejectedExecution` before any task code runs); the
exposure is the narrow race between the checks and the executor's admission,
and it ends as soon as any rush worker finishes and polls the queue.

---

## R4 — Delivery-or-throw

**Rule.** A returned `accept(m)` means the message is queued and *will* be
processed. A thrown exception means it was **not** delivered — the caller may
retry without risking a duplicate. Every real delivery failure is handed to
the actor's settable `onException` consumer and recorded on the actor
(`getException()`) before being surfaced.

**Why.** The two half-failures are the dangerous ones: silently dropping a
message the caller believes accepted, and rethrowing a failure whose message
*was* queued (a retry then delivers it twice). Both were regressions in
earlier revisions.

**Enforced by.** The counter is incremented before the put and rolled back in
the `finally` when the put fails; worker-start failures with a queued message
are never surfaced as failed accepts (an opportunistic rush failure is not an
actor error at all — it is logged at `FINE` and must not overwrite
`getException()`); `executeUntracked` fails loudly on a shut-down pool.
The failure path itself is `handleException` → the settable `onException`
consumer (default: log at `WARNING`) plus the record in `getException()` —
the protected `exception(Exception)` hook and `dryLogger()` are gone with
the logging rework.

---

## R5 — Ordering: FIFO by enqueue order, nothing more, nothing less

**Rule.** Messages are processed in the order they *entered the mailbox*.
For a single-worker actor (`SingleActor`, `BatchActor`) that is full
sequencing: one message at a time, start and completion order identical.
Multi-worker actors (declared `threads > 1`) preserve dequeue order but
process concurrently: completion order is unspecified — that is their
purpose.

**Limits, stated on purpose:**

- FIFO is by *enqueue* order, not by `accept()` call order across concurrent
  producers. A producer blocked in a bounded `put` can be overtaken by a
  later caller whose put succeeds first (the in-transit window:
  `counters` are incremented before the put). Closing that window would
  require serialising all producers behind a global lock or a ticket system;
  neither is acceptable on the hot path, and Erlang/Akka give the same
  guarantee. **One producer per actor ⇒ fully deterministic.**
- No reordering is ever introduced by the framework: no LIFO steal, no
  priority, no batch re-sequencing. R1 exists precisely to keep it that way.

**Enforced by.** The channels (FIFO by construction), the single
drain-loop-per-worker shape, and R1.

---

## R6 — Backpressure lives in the channel, not in the pool

**Rule.** The only mechanism that may slow or block a producer is the
destination actor's own mailbox (bounded capacity: `put` blocks; unbuffered:
rendezvous hand-off). Pool saturation must never block the message path and
never reject a message; it only reduces how many extra workers exist.

For **user-submitted tasks** (`execute`/`submit`/`spawn`/`forEach` on a hub)
the semantics are the complementary one: a task is never dropped and never
runs in the caller — when the pool cannot admit it, the submitting thread
waits until a worker accepts it. The wait alternates a spawn attempt with a
bounded taker-wait on the pool's hand-off queue, is **not abortable by
interruption** (the interrupt is consumed), and the only rejection is a pool
that is shut down.

**Why.** A `SynchronousQueue`-based pool whose threads all sit *inside*
long-lived tasks has no thread polling its queue: a blocking saturation
policy on the message path would wait for a thread that may never come
(hand-off requires a poller). The channel is where backpressure is
meaningful, sized, and understood by the user.

**Enforced by.** R1 (no inline work), R2/R3 (worker starts degrade to skip
or dedicated threads), and the hub's pool: it is built with an abort-style
saturation policy, every framework call site resolves a rejection
explicitly, and user submissions re-admit by blocking until accepted
(`onSubmissionRejected` → `blockUntilAccepted`). The plain `ActorPool`
keeps the caller-runs/caller-waits knob for standalone use; no framework
path depends on any policy.

---

## R7 — Shutdown preserves in-flight work

**Rule.** `shutdown(true)` drains the whole registered graph before closing:
it waits until every actor is idle, re-passing while the hub-wide processed
counter keeps advancing (in-flight forwards between stages are not lost), and
then closes stages upstream-first (topological order from the link graph),
each stage deferred-close-when-idle and awaited before the next one opens.
The wait is deliberately unbounded: its whole purpose is that in-flight work
is never dropped. `shutdown(false)` starts closing immediately; work
submitted to a terminated stage afterwards fails loudly (R4).

**Enforced by.** `ActorHub.shutdown(boolean)` (quiescence loop + cascade
order), the actor-level deferred close (`shutdownWhenEmpty` honoured at the
zero-crossing), `terminate()` running exactly once after the drain, and
`isTerminated()`/`awaitTermination()` as the observable completion signals.

---

## R8 — Interruptions are stopped and routed to `onException`

**Rule.** The framework stops every interruption it handles and hands the
caught exception to the `onException` consumer of the context it belongs
to: an interruption raised by the machinery of an actor (a producer's put,
a worker's get) or by the pool goes to `handleException` of the **hub**; an
interruption escaping `receive(m)` while the message is being processed
goes to `handleException` of the **actor** and is recorded in
`getException()`. No synthetic exceptions are ever created — only the ones
the framework already captures. Both `onException` consumers can be set at
any time (`setOnException`, fluent), and both default to logging the event.

---

## The pool-sizing rules (how R2/R3/R6 materialise in `ActorHub`)

- The effective **core** covers one permanently alive thread per registered
  non-synchronous actor (the permanent workers) on top of the configured
  `coreThreads` floor.
- The effective **maximum** covers the actors' summed declared demand,
  bounded by the configured `maxThreads` ceiling.
- The sole exception: when more actors are registered than `maxThreads`, the
  ceiling is lifted to `asyncActorCount + maxThreads` — one thread per actor
  is guaranteed (R3), and the actors' remaining demand is a target honoured
  within a rush budget of `maxThreads` extra threads. When the budget is
  exhausted, rush starts are skipped (the queued messages wait for the
  permanent workers, per R1/R2), and the rush threads created above the core
  are non-core, reclaimed after the keep-alive time.

## Saturation ladder (what happens when everything is full)

1. Actor channel fills → producers block in `put` (R6: intended
   backpressure).
2. Pool saturates → lazy rush starts are skipped; queued messages wait for
   the permanent worker (R1 + R2).
3. A permanent worker cannot be admitted → dedicated thread (R3).
4. A user-submitted task cannot be admitted → the submitting thread waits
   until a worker accepts it (R6).
5. Pool shut down → `accept` fails loudly (R4).

At no point does a message get dropped, reordered, or executed by a producer.

---

## The flavours

| Flavour | Workers | Ordering | Producer role |
|---|---|---|---|
| `SynchronousActor` | 0 — runs in the caller | sequential per caller | **is** the worker (R1 exemption) |
| `SingleActor` | 1 permanent | full sequencing | enqueues only |
| `MultiActor` | 1 permanent + up to N−1 rush | dequeue order; concurrent completion | enqueues only |
| `BatchActor` | 1 permanent drainer | batches of `maxSize` / `maxWait` | enqueues only |
| `PipeActor`/`FilterActor`/`FanOutActor`/`PipelineActor` | wrap an inner actor | inherit the inner | enqueues only |

## Mechanisms inventory

- **`CloseableChannel` family** — the mailboxes; FIFO; close drains buffered
  elements and aborts in-flight puts loudly.
- **`AtomicCounterPair`** — `queued`/`in-flight` packed in one word: the
  queue-to-processing move and the all-zero detection are single CAS
  operations; specialised primitives (no lambda/boxing) on the hot path.
- **Permanent worker** — parked on `channel.get()` for the actor's lifetime;
  never reclaimed while the actor is registered; exits only via close.
- **Lazy rush** — extra workers only on backlog (`first > 1`); bounded by the
  `workerSlots` semaphore at `threads`; slots released only after the worker
  drained its channel (a released slot implies no queued work).
- **Idle signalling on demand** — `waitForIdle()` parkers register in
  `idleWaiters`; the zero-crossing epilogue skips the lock+signal entirely
  when nobody waits. `shutdownWhenEmpty` is checked lock-free (volatile);
  the hub's cascading close is the backstop.
- **`TaskTracker`** — the pool's active-task counter (replaced a `Phaser`):
  pre-register before submission so queued tasks are visible, signal only on
  the transition to zero.
- **Quiescence drain** — `processedCount` (hub-wide `LongAdder`) + per-actor
  idle passes + topological cascade (upstream-first) close the graph without
  losing in-flight forwards.
- **Delivery accounting** — counters before put, rollback on failure; worker
  lifecycle (`activeWorkers`, `workerSlots`) purely atomic/semaphore-based;
  the actor lock is off the producer path and off the steady-state consumer
  path.

## Verification

- `nut-base-commons` test suite, in particular the actor package's race
  tests (`ActorShutdownWhenEmptyRaceTest`, `ActorAcceptContractTest`,
  `ActorInterruptedChannelTest`, `ShutdownCascadingTest`, ...).
- A 30-round stress: concurrent producers vs concurrent
  `shutdown(false)/shutdown(true)`, asserting delivery-or-throw, zero lost
  messages and complete termination.
- Benchmarks under `/tmp` tooling during development; the performance-relevant
  history lives in the commit messages (producer-path lock removal ×1.2–1.4,
  lazy rush ×2, idle gate ~−7%, tracker ~−8% on task churn).

## Status of the rules in the current code

| Rule | Status |
|---|---|
| R1–R8 | enforced |
