(ns cdq.create.files)

(defn do! [{:keys [ctx/gdx]
            :as ctx}]
  (assoc ctx :ctx/files (:clojure.gdx/files gdx)))
