(ns cdq.application.render.when-not-paused
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.potential-field.update :as potential-field]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :refer [bind-root
                               pretty-pst]]
            [gdl.graphics :as graphics]
            [gdl.ui :as ui]))

(defn- update-time! []
  (let [delta-ms (min (graphics/delta-time) ctx/max-delta)]
    (alter-var-root #'ctx/elapsed-time + delta-ms)
    (bind-root #'ctx/delta-time delta-ms)))

(defn- update-potential-fields! []
  (doseq [[faction max-iterations] ctx/factions-iterations]
    (potential-field/tick! ctx/potential-field-cache
                           ctx/grid
                           faction
                           ctx/active-entities
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
               (ctx/handle-txs! (entity/tick! [k v] eid ctx)))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (pretty-pst t)
     (ui/add! stage (error-window/create t))
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn do! []
  (when-not ctx/paused?
    (update-time!)
    (update-potential-fields!)
    (tick-entities! {:ctx/active-entities ctx/active-entities
                     :ctx/elapsed-time ctx/elapsed-time
                     :ctx/delta-time ctx/delta-time
                     :ctx/grid ctx/grid
                     :ctx/stage ctx/stage})))
