(ns anvil.world.tick.entities
  (:require [anvil.component :as component]
            [anvil.world :as world]
            [anvil.world.tick :as tick]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl bind-root]]))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (component/tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn-impl tick/entities []
  (try (run! tick-entity (world/active-entities))
       (catch Throwable t
         (stage/error-window! t)
         (bind-root world/error t))))
