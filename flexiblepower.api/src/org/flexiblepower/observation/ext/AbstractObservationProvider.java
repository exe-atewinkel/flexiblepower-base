package org.flexiblepower.observation.ext;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.flexiblepower.observation.Observation;
import org.flexiblepower.observation.ObservationConsumer;
import org.flexiblepower.observation.ObservationProvider;

/**
 * Gives a basic implementation of an {@link ObservationProvider} where the {@link #subscribe(ObservationConsumer)} and
 * {@link #unsubscribe(ObservationConsumer)} methods are implemented. To publish a new observation, the
 * {@link #publish(Observation)} method should be used.
 *
 * @param <T>
 *            The type of the value
 */
public abstract class AbstractObservationProvider<T> implements ObservationProvider<T> {

    private final Set<ObservationConsumer<? super T>> consumers =
                                                                new CopyOnWriteArraySet<ObservationConsumer<? super T>>();
    private final AtomicReference<Observation<? extends T>> lastObservation =
                                                                            new AtomicReference<Observation<? extends T>>(null);
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    @Override
    public void subscribe(ObservationConsumer<? super T> consumer) {
        consumers.add(consumer);
    }

    @Override
    public void unsubscribe(ObservationConsumer<? super T> consumer) {
        consumers.remove(consumer);
    }

    @Override
    public Observation<? extends T> getLastObservation() {
        return lastObservation.get();
    }

    /**
     * Publishes an observation to all the subscribed consumers.
     *
     * @param observation
     *            The observation that will be sent.
     */
    public void publish(final Observation<? extends T> observation) {
        lastObservation.set(observation);
        for (final ObservationConsumer<? super T> consumer : consumers) {
            // Call each consumer in a separate thread through the ExecutorService so any exception or blocking
            // behaviour in a consumer will not affect the main thread or calls to other consumers
            final Runnable task = new Runnable() {
                @Override
                public void run() {
                    consumer.consume(AbstractObservationProvider.this, observation);
                }
            };
            executorService.submit(task);
        }
    }
}
