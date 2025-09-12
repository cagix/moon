(ns cdq.render.clear-screen
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.screen :as screen]))

(defn do!
  [_ctx]
  (screen/clear! color/black))
