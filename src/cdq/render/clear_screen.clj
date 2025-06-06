(ns cdq.render.clear-screen
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.screen :as screen-utils]))

(defn do! [ctx]
  (screen-utils/clear! (color/create :black))
  ctx)
