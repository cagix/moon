(ns cdq.start.context-record
  (:require [qrecord.core :as q]))

(q/defrecord Context [
                      ctx/graphics
                      ])
(defn do! [ctx]
  (merge (map->Context {})
         ctx))
