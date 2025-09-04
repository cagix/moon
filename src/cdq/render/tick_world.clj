(ns cdq.render.tick-world
  (:require [cdq.ctx :as ctx]
            [cdq.gdx.graphics :as graphics]
            [cdq.stacktrace :as stacktrace]
            [cdq.world.potential-fields.update :as potential-fields.update]
            [cdq.ui.stage :as stage]
            [cdq.ui.error-window :as error-window]))

(declare entity->tick)

(defn- update-time* [{:keys [world/max-delta] :as world} delta-ms]
  (let [delta-ms (min delta-ms max-delta)]
    (-> world
        (assoc :world/delta-time delta-ms)
        (update :world/elapsed-time + delta-ms))))

(defn- update-time [{:keys [ctx/gdx-graphics
                            ctx/world]
                     :as ctx}]
  (update ctx :ctx/world update-time* (graphics/delta-time gdx-graphics)))

(defn- update-potential-fields!
  [{:keys [ctx/factions-iterations
           ctx/potential-field-cache
           ctx/world]
    :as ctx}]
  (let [{:keys [world/grid
                world/active-entities]} world]
    (doseq [[faction max-iterations] factions-iterations]
      (potential-fields.update/tick! potential-field-cache
                                     grid
                                     faction
                                     active-entities
                                     max-iterations)))
  ctx)

; (defmulti tick! (fn [[k] _v _eid _world]
;                   k))
; (defmethod tick! :default [_ _v _eid _world])

(defn- tick-component! [k v eid world]
  (when-let [f (entity->tick k)]
    (f v eid world)))

(defn- tick-entity! [{:keys [ctx/world] :as ctx} eid]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (ctx/handle-txs! ctx (tick-component! k v eid world)))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (:entity/id @eid)}
                           t))))))

(defn- tick-entities!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (try
   (doseq [eid (:world/active-entities world)]
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
