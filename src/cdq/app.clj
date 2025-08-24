(ns cdq.app
  (:require [clj-commons.pretty.repl :as pretty-repl]
            [cdq.malli :as m]))

(defn create [_ctx {:keys [schema stacktraces]}]
  {:cdq.app/runnables []
   :cdq.app/schema (m/schema schema)
   :cdq.app/stacktraces stacktraces})

(defn validate [{:keys [ctx/app] :as ctx}]
  (m/validate-humanize (:cdq.app/schema app) ctx)
  ctx)

(defn add-runnable [ctx runnable]
  (update-in ctx [:ctx/app :cdq.app/runnables] conj runnable))

(defn run-runnables! [{:keys [ctx/app] :as ctx}]
  (doseq [runnable (:cdq.app/runnables app)]
    (runnable ctx))
  (assoc-in ctx [:ctx/app :cdq.app/runnables] []))

(defn pretty-pst [{:keys [cdq.app/stacktraces]} t]
  (let [{:keys [print-level
                print-depth]} stacktraces]
    (binding [*print-level* print-level]
      (pretty-repl/pretty-pst t print-depth))))
