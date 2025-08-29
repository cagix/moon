(ns cdq.app
  (:require [clj-commons.pretty.repl :as pretty-repl]))

(defn create [{:keys [stacktraces]}]
  {:cdq.app/runnables []
   :cdq.app/stacktraces stacktraces})

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
