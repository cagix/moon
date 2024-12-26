(ns anvil.world.tick.entities
  (:require [anvil.component :as component]
            [anvil.world.tick :as tick]
            [cdq.context :as world]
            [gdl.context :as c]))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [c eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (component/tick [k v] eid c))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn-impl tick/entities [c]
  (try (run! #(tick-entity c %) (world/active-entities c))
       (catch Throwable t
         (c/error-window c t)
         #_(bind-root world/error t))) ; FIXME ... either reduce or use an atom ...
  c)
