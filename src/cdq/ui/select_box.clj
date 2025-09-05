(ns cdq.ui.select-box
  (:require cdq.construct)
  (:import (com.kotcrab.vis.ui.widget VisSelectBox)))

(defmethod cdq.construct/construct :actor.type/select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(def get-selected VisSelectBox/.getSelected)
