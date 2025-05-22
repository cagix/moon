(ns cdq.render.when-not-paused
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.potential-field.update :as potential-field]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :refer [pretty-pst]]
            [gdl.graphics :as graphics]
            [gdl.ui :as ui]))

(defn- update-potential-fields! [{:keys [ctx/potential-field-cache
                                         ctx/grid
                                         ctx/active-entities]}]
  (doseq [[faction max-iterations] ctx/factions-iterations]
    (potential-field/tick! potential-field-cache
                           grid
                           faction
                           active-entities
                           max-iterations)))

(defn- tick-entities!
  [{:keys [ctx/active-entities
           ctx/stage]
    :as ctx}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (g/handle-txs! ctx (entity/tick! [k v] eid ctx)))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (pretty-pst t)
     (ui/add! stage (error-window/create t))
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn assoc-delta-time
  [ctx]
  (assoc ctx :ctx/delta-time (min (graphics/delta-time) ctx/max-delta)))

(defn update-elapsed-time
  [{:keys [ctx/delta-time]
    :as ctx}]
  (update ctx :ctx/elapsed-time + delta-time))

(defn do! [{:keys [ctx/paused?] :as ctx}]
  (if paused?
    ctx
    (let [ctx (-> ctx
                  assoc-delta-time
                  update-elapsed-time)]
      (update-potential-fields! ctx)
      (tick-entities! ctx)
      ctx)))
