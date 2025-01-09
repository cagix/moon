(ns gdl.graphics
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.screen :as screen]
            [gdl.ui :as ui]))

(defn clear-screen [context]
  (screen/clear color/black)
  context)

(defn draw-stage [{:keys [gdl.context/stage] :as context}]
  (ui/draw stage (assoc context :gdl.context/unit-scale 1))
  context)
