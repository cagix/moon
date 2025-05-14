(ns cdq.game.tick-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.stage :as stage]
            [cdq.utils :as utils]))

(defn do! []
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid (:active-entities ctx/world)]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (utils/handle-txs! (entity/tick! [k v] eid)))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (utils/pretty-pst t)
     (stage/show-error-window! ctx/stage t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )
