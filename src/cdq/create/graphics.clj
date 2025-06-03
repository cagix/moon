(ns cdq.create.graphics
  (:require [gdl.gdx :as gdx]))

(defn do! [ctx]
  (assoc ctx :ctx/graphics (gdx/graphics)))
