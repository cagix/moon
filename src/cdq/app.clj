(ns cdq.app)

(defn create []
  {:cdq.app/runnables []})

(defn add-runnable [ctx runnable]
  (update-in ctx [:ctx/app :cdq.app/runnables] conj runnable))

(defn run-runnables! [{:keys [ctx/app] :as ctx}]
  (doseq [runnable (:cdq.app/runnables app)]
    (runnable ctx))
  (assoc-in ctx [:ctx/app :cdq.app/runnables] []))
