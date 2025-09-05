(ns cdq.ui.select-box
  (:require [clojure.gdx.scenes.scene2d.actor :as actor])
  (:import (com.kotcrab.vis.ui.widget VisSelectBox)))

(defmethod actor/construct :actor.type/select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(def get-selected VisSelectBox/.getSelected)
