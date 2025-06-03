(ns cdq.create.files
  (:require [gdl.gdx :as gdx]))

(defn do! [ctx]
  (assoc ctx :ctx/files (gdx/files)))
