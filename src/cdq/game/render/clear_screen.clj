(ns cdq.game.render.clear-screen
  (:require [cdq.graphics.color :as color]
            [clojure.gdx :as gdx]))

(defn step
  [{:keys [ctx/gdx] :as ctx}]
  (gdx/clear! gdx color/black)
  ctx)
