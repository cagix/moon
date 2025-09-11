(ns cdq.render.tick-entities
  (:require [cdq.ctx :as ctx]))

(defn- tick-component!
  [k v eid {:keys [ctx/entity-components]
            :as ctx}]
  (when-let [f ((:tick entity-components) k)]
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
  [{:keys [ctx/active-entities]
    :as ctx}]
  (doseq [eid active-entities]
    (tick-entity! ctx eid)))

(defn- do!*
  [{:keys [ctx/stage]
    :as ctx}]
  (try
   (tick-entities! ctx)
   (/ 1 0)
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
