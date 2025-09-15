(ns cdq.render.tick-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]))

(defn- tick-entity! [ctx eid]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (ctx/handle-txs! ctx (entity/tick [k v] eid ctx)))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (:entity/id @eid)}
                           t))))))

(defn- tick-entities!
  [{:keys [ctx/world]
    :as ctx}]
  (doseq [eid (:world/active-entities world)]
    (tick-entity! ctx eid)))

(defn- do!*
  [{:keys [ctx/stage]
    :as ctx}]
  (try
   (tick-entities! ctx)
   (catch Throwable t
     (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                           [:tx/show-error-window t]])
     #_(bind-root ::error t)))
  ctx)

(defn do!
  [ctx]
  (if (:ctx/paused? ctx)
    ctx
    (do!* ctx)))
