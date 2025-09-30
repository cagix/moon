(ns cdq.ctx.create-record
  (:require [qrecord.core :as q]))

(q/defrecord Context [])

(defn do! [ctx]
  (merge (map->Context {})
         ctx))
