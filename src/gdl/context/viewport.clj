(ns gdl.context.viewport
  (:require [clojure.gdx :as gdx]))

(defn create [[_ {:keys [width height]}] _c]
  (gdx/fit-viewport width height (gdx/orthographic-camera)))

(defn resize [[_ viewport] w h]
  (gdx/resize viewport w h :center-camera? true))
