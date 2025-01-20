(ns cdq.render.when-not-paused.tick-entities
  (:require cdq.error
            cdq.world))

(defn render [{:keys [cdq.game/active-entities] :as context}]
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
  context)
