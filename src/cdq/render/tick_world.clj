(ns cdq.render.tick-world
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.stacktrace :as stacktrace]
            [cdq.potential-fields.update :as potential-fields.update]
            [cdq.ui.stage :as stage]
            [cdq.ui.error-window :as error-window]))

(declare entity->tick)

(defn- update-time
  [{:keys [ctx/graphics
           ctx/max-delta]
    :as ctx}]
  (let [delta-ms (min (graphics/delta-time graphics) max-delta)]
    (-> ctx
        (assoc :ctx/delta-time delta-ms)
        (update :ctx/elapsed-time + delta-ms))))

(defn- update-potential-fields!
  [{:keys [ctx/factions-iterations
           ctx/potential-field-cache
           ctx/grid
           ctx/active-entities]
    :as ctx}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations))
  ctx)

(defn- tick-component! [k v eid ctx]
  (when-let [f (entity->tick k)]
    (f v eid ctx)))

(defn- tick-entity! [ctx eid]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (ctx/handle-txs! ctx (tick-component! k v eid ctx)))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (:entity/id @eid)}
                           t))))))

(defn- tick-entities!
  [{:keys [ctx/stage
           ctx/active-entities]
    :as ctx}]
  (try
   (doseq [eid active-entities]
     (tick-entity! ctx eid))
   (catch Throwable t
     (stacktrace/pretty-print t)
     (stage/add! stage (error-window/create t))
     #_(bind-root ::error t)))
  ctx)

(defn do!
  [ctx]
  (if (:ctx/paused? ctx)
    ctx
    (-> ctx
        update-time
        update-potential-fields!
        tick-entities!)))
