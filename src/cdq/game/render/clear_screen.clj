(ns cdq.game.render.clear-screen
  (:require [clojure.color :as color]
            [clojure.gdx :as gdx]))

(defn step
  [{:keys [ctx/gdx] :as ctx}]
  (gdx/clear! gdx color/black)
  ctx)
