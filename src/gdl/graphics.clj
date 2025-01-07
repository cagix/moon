(ns gdl.graphics
  (:require [clojure.gdx :as gdx]
            [gdl.ui :as ui]))

(defn clear-screen [context]
  (gdx/clear-screen gdx/black)
  context)

(defn draw-stage [{:keys [gdl.context/stage] :as context}]
  (ui/draw stage (assoc context :gdl.context/unit-scale 1))
  context)
