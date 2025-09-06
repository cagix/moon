(ns cdq.create.graphics
  (:require [clojure.gdx :as gdx]))

(defn do! [ctx]
  (assoc ctx :ctx/graphics (gdx/graphics)))
