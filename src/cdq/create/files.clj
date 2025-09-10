(ns cdq.create.files
  (:require [clojure.gdx :as gdx]))

(defn do! [ctx]
  (assoc ctx :ctx/files (gdx/files)))
