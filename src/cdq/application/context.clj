(ns cdq.application.context
  (:require [cdq.malli :as m]
            [qrecord.core :as q]))

(defn create [ks]
  ; I could even create a namespace here or do in a namespace ...
  (in-ns 'cdq.application.context) ; otherwise 'user TODO return back to previous ns afterwards
  (eval `(q/defrecord ~'Context ~(mapv (comp symbol first) ks)))
  (def schema (m/schema (apply vector :map {:closed true} ks)))
  (eval `(map->Context {})))

(defn validate [ctx]
  (m/validate-humanize schema ctx)
  ctx)
