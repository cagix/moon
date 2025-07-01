(ns gdl.app
  (:require [clj-commons.pretty.repl :as pretty-repl]
            [gdl.malli :as m]))

(defn create [_ctx {:keys [schema stacktraces]}]
  {:gdl.app/runnables []
   :gdl.app/schema (m/schema schema)
   :gdl.app/stacktraces stacktraces})

(defn validate [{:keys [ctx/app] :as ctx}]
  (m/validate-humanize (:gdl.app/schema app) ctx)
  ctx)

(defn add-runnable [ctx runnable]
  (update-in ctx [:ctx/app :gdl.app/runnables] conj runnable))

(defn run-runnables! [{:keys [ctx/app] :as ctx}]
  (doseq [runnable (:gdl.app/runnables app)]
    (runnable ctx))
  (assoc-in ctx [:ctx/app :gdl.app/runnables] []))

(defn pretty-pst [{:keys [gdl.app/stacktraces]} t]
  (let [{:keys [print-level
                print-depth]} stacktraces]
    (binding [*print-level* print-level]
      (pretty-repl/pretty-pst t print-depth))))
