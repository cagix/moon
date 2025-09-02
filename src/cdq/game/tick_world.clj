(ns cdq.game.tick-world
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.graphics :as graphics]
            [cdq.ctx.world :as world]
            [cdq.stacktrace :as stacktrace]
            [cdq.world.entity :as entity]
            [cdq.ui.stage :as stage]
            [cdq.ui.error-window :as error-window]))

(declare entity->tick)

(defn- update-time [{:keys [ctx/graphics
                            ctx/world]
                     :as ctx}]
  (update ctx :ctx/world world/update-time (graphics/delta-time graphics)))

(defn- update-potential-fields!
  [{:keys [ctx/world]
    :as ctx}]
  (world/tick-potential-fields! world)
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
                            :entity/id (entity/id @eid)}
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
  (if (get-in ctx [:ctx/world :world/paused?])
    ctx
    (-> ctx
        update-time
        update-potential-fields!
        tick-entities!)))
