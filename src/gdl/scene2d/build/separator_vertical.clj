(ns gdl.scene2d.build.separator-vertical
  (:require [com.kotcrab.vis.ui.widget.separator :as separator]
            [gdl.scene2d :as scene2d]))

(defmethod scene2d/build :actor.type/separator-vertical [_]
  (separator/vertical))
