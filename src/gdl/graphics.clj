(ns gdl.graphics
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [gdl.ui :as ui]))

(defn clear-screen [context]
  (gdx/clear-screen color/black)
  context)

(defn draw-stage [{:keys [gdl.context/stage] :as context}]
  (ui/draw stage (assoc context :gdl.context/unit-scale 1))
  context)
