(ns cdq.render.clear-screen
  (:require [gdl.gdx :as gdx]))

(defn do! [ctx]
  (gdx/clear-screen!)
  ctx)
