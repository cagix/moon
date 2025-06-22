(ns gdl.app
  (:require [cdq.malli :as m]))

(defn create [ctx {:keys [schema]}]
  {:gdl.app/runnables []
   :gdl.app/schema (m/schema schema)})

(defn validate [{:keys [ctx/app] :as ctx}]
  (m/validate-humanize (:gdl.app/schema app) ctx)
  ctx)

(defn add-runnable [ctx runnable]
  (update-in ctx [:ctx/app :gdl.app/runnables] conj runnable))

(defn run-runnables! [{:keys [ctx/app] :as ctx}]
  (doseq [runnable (:gdl.app/runnables app)]
    (runnable ctx))
  (assoc-in ctx [:ctx/app :gdl.app/runnables] []))
