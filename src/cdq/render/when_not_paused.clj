(ns cdq.render.when-not-paused
  (:require cdq.error
            cdq.potential-fields
            cdq.time
            cdq.world
            clojure.gdx.graphics))

(defn render [context]
  (if (:cdq.context/paused? context)
    context
    (reduce (fn [context f] (f context))
            context
            [(fn [context]
               (let [delta-ms (min (clojure.gdx.graphics/delta-time)
                                   cdq.time/max-delta)]
                 (-> context
                     (update :cdq.context/elapsed-time + delta-ms)
                     (assoc :cdq.context/delta-time delta-ms))))
             (fn [{:keys [cdq.context/factions-iterations
                          cdq.context/grid
                          world/potential-field-cache
                          cdq.game/active-entities]
                   :as context}]
               (doseq [[faction max-iterations] factions-iterations]
                 (cdq.potential-fields/tick potential-field-cache
                                                grid
                                                faction
                                                active-entities
                                                max-iterations))
               context)
             (fn [{:keys [cdq.game/active-entities] :as context}]
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
                            (cdq.world/tick! [k v] eid context))
                          (catch Throwable t
                            (throw (ex-info "entity-tick" {:k k} t)))))
                   (catch Throwable t
                     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
                (catch Throwable t
                  (cdq.error/error-window context t)
                  #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
               context)])))
