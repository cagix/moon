(ns clojure.vis-ui.window
  (:require [clojure.gdx.scene2d.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Label
                                               Window)
           (com.kotcrab.vis.ui.widget VisWindow)))

; TODO buggy FIXME
(defn title-bar?
  "Returns true if the actor is a window title bar."
  [actor]
  (when (instance? Label actor)
    (when-let [p (actor/parent actor)]
      (when-let [p (actor/parent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))
