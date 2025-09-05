(ns cdq.ui.check-box
  (:require cdq.construct)
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)
           (com.kotcrab.vis.ui.widget VisCheckBox)))

(defmethod cdq.construct/construct :actor.type/check-box -check-box
  [{:keys [text on-clicked checked?]}]
  (let [^Button button (VisCheckBox. (str text))]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))

(def checked? VisCheckBox/.isChecked)
