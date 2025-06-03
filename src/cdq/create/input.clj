(ns cdq.create.input
  (:require [gdl.gdx :as gdx]))

(defn do! [ctx]
  (assoc ctx :ctx/input (gdx/input)))
