(ns cdq.ui.utils
  (:require [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.input-event :as input-event]
            [clojure.gdx.scenes.scene2d.utils :as utils]))

(defn click-listener [f]
  (utils/click-listener
   (fn [event _x _y]
     (f (stage/get-ctx (input-event/get-stage event))))))

(defn change-listener [on-clicked]
  (utils/change-listener
   (fn [event actor]
     (on-clicked actor (stage/get-ctx (input-event/get-stage event))))))

(def drawable utils/drawable)
