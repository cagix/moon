(ns cdq.create.input)

(defn do! [ctx]
  (assoc ctx :ctx/input (:clojure.gdx/input (:ctx/gdx ctx))))
