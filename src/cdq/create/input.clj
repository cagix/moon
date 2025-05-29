(ns cdq.create.input)

(defn do! [{:keys [ctx/gdx]
            :as ctx}]
  (assoc ctx :ctx/input (:clojure.gdx/input gdx)))
