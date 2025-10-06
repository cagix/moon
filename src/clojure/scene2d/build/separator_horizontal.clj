(ns clojure.scene2d.build.separator-horizontal
  (:require [com.kotcrab.vis-ui.widget.separator :as separator]
            [clojure.scene2d :as scene2d]))

(defmethod scene2d/build :actor.type/separator-horizontal [_]
  (separator/horizontal))
