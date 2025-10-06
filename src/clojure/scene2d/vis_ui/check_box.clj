(ns clojure.scene2d.vis-ui.check-box
  (:require [clojure.gdx.vis-ui.widget.vis-check-box :as check-box])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)))

(defn create
  [{:keys [text on-clicked checked?]}]
  (let [^Button button (check-box/create text)]
    (.setChecked button checked?)
    (.addListener button
                  (proxy [ChangeListener] []
                    (changed [event ^Button actor]
                      (on-clicked (.isChecked actor)))))
    button))
