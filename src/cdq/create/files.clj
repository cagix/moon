(ns cdq.create.files)

(defn do! [ctx]
  (assoc ctx :ctx/files (:clojure.gdx/files (:ctx/gdx ctx))))
