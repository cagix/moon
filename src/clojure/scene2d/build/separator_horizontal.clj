(ns clojure.scene2d.build.separator-horizontal
  (:require [clojure.gdx.scenes.scene2d.vis-ui.widget.separator :as separator]
            [clojure.scene2d :as scene2d]))

(defmethod scene2d/build :actor.type/separator-horizontal [_]
  (separator/horizontal))
